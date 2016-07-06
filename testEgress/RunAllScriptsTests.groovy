// RunAllScriptsTests.groovy
import groovy.util.GroovyTestSuite
import junit.framework.Test
import junit.textui.TestRunner
import org.codehaus.groovy.runtime.ScriptTestAdapter

class AllTests {
    static Test suite() {
        def allTests = new GroovyTestSuite()

        new File('.').eachFileMatch(~/Test.*\.groovy/) {
                allTests.addTest(new ScriptTestAdapter(allTests.compile(it.name), [] as String[]))
        }

        return allTests
    }
}

TestRunner.run(AllTests.suite())

