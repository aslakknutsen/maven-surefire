package org.apache.maven.surefire.its;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

public class ActivateAdditionalProfilesIT extends SurefireIntegrationTestCase
{
    public void testActivateProfileDuringTest() throws Exception
    {
        SurefireLauncher parentLauncher = unpack( "/activate-additional-profiles" );
        parentLauncher.addGoal("test").addGoal("install");
        OutputValidator outputValidator = parentLauncher.executeCurrentGoals();

        OutputValidator compileValidator = parentLauncher.getSubProjectValidator("compiletime");
        compileValidator.assertTestSuiteResults(1, 0, 0, 0);
    }
}
