import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->

		essApps = essSvr.applications

		// Show that we have a list of applications.
		assert essApps.size() > 0

		// Use each() to show all of the applications have a name.
		essApps.each { assert it.name.size() > 0 }

		// Use any() to show that there is an application with a particular name.
		assert essApps.any { it.name == 'Sample' }

		// Use every() to show that all of the applications share a characteristic.
		assert essApps.every { it.name =~ /(?i)[aeiou]/ }

		// Use collect() to show that we can build a new list out of the existing one.
		assert essApps.collect { it.name }[0] == 'Demo'

		// Use findAll() to show that we can get a criteria-based subset.
		assert essApps.findAll { it.name =~ /(?i)Samp/ }.size() == 5

		// Use find() to show that get a single item based on criteria.
		assert essApps.find { it.name =~ /(?i)Samp/ } instanceof com.essbase.api.datasource.IEssOlapApplication

		// Use inject to show we can build an object cumulatively using the elements of the list.
		assert essApps.inject('') { r, a -> r << a.name[0] }.toString()[0..5] == 'DSSSSA'
	}
}
