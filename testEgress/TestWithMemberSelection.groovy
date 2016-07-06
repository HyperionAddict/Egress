import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def i = 0

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essSvr.withApplication('Sample') { essApp ->
			essApp.withCube('Basic') { essCube ->
				essCube.withMemberSelection { essSel ->
					assert essSel instanceof com.essbase.api.metadata.IEssMemberSelection
					essSel.executeQuery('California')
					assert essSel.members[0].name == 'California'
					++i
				}
			}
		}
	}
}

assert i > 0
