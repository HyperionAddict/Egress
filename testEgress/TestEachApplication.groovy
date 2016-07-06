import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def i = 0

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essSvr.eachApplication { essApp ->
			assert essApp instanceof com.essbase.api.datasource.IEssOlapApplication
			++i
		}
	}
}

assert i > 0
