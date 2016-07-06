import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essApps = essSvr.applications
		assert essApps.size() > 0
	}
}
