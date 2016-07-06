import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def i = 0
def j = 0

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essSvr.applications.each { essApp ->
			essApp.cubes.collect{it.name}.each { cubeName ->
				essApp.withCube(cubeName) { essCube ->
					assert essCube instanceof com.essbase.api.datasource.IEssCube
					++i
				}
			}
		}
		essSvr.withCube('Sample', 'Basic') { essCube ->
			assert essCube.name == 'Basic'
			++j
		}
	}
}

assert i > 0 && j > 0
