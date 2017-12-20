import hudson.model.*
import jenkins.model.Jenkins
import jenkins.security.s2m.AdminWhitelistRule
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.BranchSpec;

println 'Configuring JNLP agent protocols'
//https://github.com/samrocketman/jenkins-bootstrap-shared/blob/master/scripts/configure-jnlp-agent-protocols.groovy
Jenkins.instance.setAgentProtocols(['JNLP4-connect', 'Ping'] as Set<String>)
Jenkins.instance.save()

println 'Configuring CSRF protection'
//https://github.com/samrocketman/jenkins-bootstrap-shared/blob/master/scripts/configure-csrf-protection.groovy
Jenkins.instance.setCrumbIssuer(new hudson.security.csrf.DefaultCrumbIssuer(true))
Jenkins.instance.save()

println 'Configuring Slave to Master Access Control'
//https://github.com/samrocketman/jenkins-bootstrap-shared/blob/master/scripts/security-disable-agent-master.groovy
//https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control
Jenkins.instance.getInjector().getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false)
Jenkins.instance.save()


println 'Creating/Updating \'github-webhook\' job'
def ghwhJobName='github-webhook';
def ghwhJob=Jenkins.instance.getItem(ghwhJobName);
def ghwhAction='update'
if (ghwhJob==null){
  ghwhAction='add'
  ghwhJob = new WorkflowJob(Jenkins.instance, ghwhJobName);
}

def ghwhJobScm = new GitSCM("https://github.com/cvarjao/openshift-jenkins-tools.git");
ghwhJobScm.branches = [new BranchSpec("*/master")];

ghwhJob.definition = new CpsScmFlowDefinition(ghwhJobScm, "github-webhook/Jenkinsfile");
ghwhJob.definition.setLightweight(true);


def payloadParameter = new StringParameterDefinition('payload', '{}', 'Github webhook payload')
def jobParameters = ghwhJob.getProperty(ParametersDefinitionProperty.class)

if (jobParameters == null) {
    def newArrList = new ArrayList<ParameterDefinition>(1)
    newArrList.add(payloadParameter)
    def newParamDef = new ParametersDefinitionProperty(newArrList)
    ghwhJob.addProperty(newParamDef)
}
else {
    // Parameters exist! We should check if this one exists already!
    if (jobParameters.parameterDefinitions.find({ it.name == 'payload' }) == null) {
        jobParameters.parameterDefinitions.add(payloadParameter)
    }
}
    
if ('add'.equals(ghwhAction)){
  Jenkins.instance.add(ghwhJob, ghwhJob.name);
  println 'job \'github-webhook\' has been created'
}else{
  ghwhJob.save()
  println 'job \'github-webhook\' has been updated'
}
