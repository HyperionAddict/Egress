import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def i = 0

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essSvr.withApplication('Sample') { essApp ->
			essApp.withCube('Basic') { essCube ->
				essCube.withOutline { essOtl ->
					essOtl.eachMember { essMbr ->
//println "${'\t' * (essMbr.generationNumber - 1)}${essMbr.name}(${essMbr.getAlias(null)})[${essMbr.shareOption}]"
						assert essMbr instanceof com.essbase.api.metadata.IEssMember
						++i
					}
				}
			}
		}
	}
}

assert i > 0
