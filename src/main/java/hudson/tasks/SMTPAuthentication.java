package hudson.tasks;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SMTPAuthentication extends AbstractDescribableImpl<SMTPAuthentication> {

    /** The ID of the Jenkins Username/Password credential to use. */
    private String credentialsId;

    @DataBoundConstructor
    public SMTPAuthentication(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
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
                    .includeMatchingAs(null, (Item) null, StandardUsernamePasswordCredentials.class, null, null)
                    .includeCurrentValue(credentialsId);
        }
    }
}
