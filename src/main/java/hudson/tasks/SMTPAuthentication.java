package hudson.tasks;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SMTPAuthentication extends AbstractDescribableImpl<SMTPAuthentication> {

    private static final Logger LOGGER = Logger.getLogger(SMTPAuthentication.class.getName());

    private static final int NUM_RETRIES = 10;

    /** use StandardUsernamePasswordCredentials instead */
    @Deprecated
    private transient String username;

    /** use StandardUsernamePasswordCredentials instead */
    @Deprecated
    private transient Secret password;

    /** The ID of the Jenkins Username/Password credential to use. */
    private String credentialsId;

    @Deprecated
    public SMTPAuthentication(String username, Secret password) {
        this.username = username;
        this.password = password;
    }

    @DataBoundConstructor
    public SMTPAuthentication(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    private Object readResolve() {
        if (username != null || password != null) {
            LOGGER.log(Level.CONFIG, "Migrating the Mailer SMTP authentication details to credential...");

            final CredentialsStore store = ExtensionList.lookup(CredentialsStore.class).get(SystemCredentialsProvider.StoreImpl.class);

            if (store == null) {
                throw new RuntimeException("Could not migrate the Mailer SMTP authentication details to credential, as the system credentials provider was missing.");
            }

            final boolean isSuccess = retry(NUM_RETRIES, (attempt) -> {
                LOGGER.log(Level.CONFIG, "Attempt {}/{}...", new Object[]{ attempt, NUM_RETRIES});

                final String id = UUID.randomUUID().toString();

                final StandardUsernamePasswordCredentials migratedCredential = new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL,
                        id,
                        "Mailer SMTP authentication credentials (migrated)",
                        username,
                        password.getEncryptedValue());

                store.addCredentials(Domain.global(), migratedCredential);

                LOGGER.log(Level.CONFIG, "Successfully migrated the Mailer SMTP authentication details to the credential '{}'", id);
            });

            if (!isSuccess) {
                throw new RuntimeException("All attempts to migrate the Mailer SMTP authentication details failed");
            }

            username = null;
            password = null;
        }

        return this;
    }

    private interface Retryable {
        void run(int attempt) throws Exception;
    }

    private static boolean retry(int times, Retryable fn) {
        for (int attempt = 1; attempt <= times; ++attempt) {
            try {
                fn.run(attempt);

                return true;
            } catch (Exception e) {
                // try again
            }
        }

        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SMTPAuthentication> {

        @Override
        public String getDisplayName() {
            return "Use SMTP Authentication";
        }

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result
                    .includeMatchingAs(ACL.SYSTEM, (Item) null, StandardUsernamePasswordCredentials.class, Collections.emptyList(), CredentialsMatchers.always())
                    .includeCurrentValue(credentialsId);
        }
    }
}
