import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def i = 0

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essSvr.applications.each { essApp ->
			essApp.eachCube { essCube ->
				assert essCube instanceof com.essbase.api.datasource.IEssCube
				++i
			}
		}
	}
}

assert i > 0
