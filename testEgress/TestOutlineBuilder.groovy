import com.hyperionaddict.egress.OutlineBuilder
import com.essbase.api.session.IEssbase
import com.essbase.api.metadata.IEssMember.EEssConsolidationType
import com.essbase.api.metadata.IEssMember.EEssShareOption
import com.essbase.api.datasource.IEssOlapApplication.EEssDataStorageType

run 'ConfigTests.groovy' as File

com.hyperionaddict.egress.Egress.withServer(signOn) { essSvr ->
	essSvr.getApplicationOrNull("zTstApp")?.delete()
}

def otlBld = new OutlineBuilder()

otlBld.signOn = signOn

otlBld.application("zTstApp") {
	cube("zTstCube") {
		dimension("TestDim1") {
			member("TestMbr1.1", consolidationType: EEssConsolidationType.SUBTRACTION)
			member("TestMbr1.2", consolidationType: EEssConsolidationType.IGNORE) {
				member("TestMbr1.2.1")
				member("TestMbr1.1", shareOption: EEssShareOption.SHARED_MEMBER)
				member("TestMbr1.2.2")
			}
			member("TestMbr1.3", consolidationType: EEssConsolidationType.IGNORE)
		}
		dimension("TestDim2") {
			member("TestMbr2.1")
			member("TestMbr2.2")
		}
		dimension("TestDim3") {
			member("TestMbr3.1")
		}
	}
}
//return
def (i, j, k) = [0, 0, 0]

IEssbase.withServer(signOn) { essSvr ->
	essSvr.withApplication('zTstApp') { essApp ->
		try {
			essApp.withOutline('zTstCube') { essOtl ->
				essOtl.eachMember { essMbr ->
					assert essMbr instanceof com.essbase.api.metadata.IEssMember
					if (essMbr.name == 'TestMbr1.2') {
						assert essMbr.consolidationType == EEssConsolidationType.IGNORE
						assert essMbr.relatedMemberNames == ["TestDim1", "TestMbr1.1", "TestMbr1.3", "TestMbr1.2.1"]
						++i
					}
					if (essMbr.shareOption == EEssShareOption.SHARED_MEMBER) {
						assert essMbr.relatedMemberNames == ["TestMbr1.2", "TestMbr1.2.1", "TestMbr1.2.2", ""]
						++j
					}
					if (essMbr.name == 'TestDim2') {
						assert essMbr.relatedMemberNames == ["", "TestDim1", "TestDim3", "TestMbr2.1"]
						++k
					}
				}
			}
		} finally {
			essApp.delete()
		}
	}
}

assert i > 0
assert j > 0
assert k > 0

/* Do it again, ASO style! */

otlBld.application("zTstApp", storageType: EEssDataStorageType.ASO ) {
	cube("zTstCube") {
		dimension("TestDim1") {
			member("TestMbr1.1")
			member("TestMbr1.2") {
				member("TestMbr1.2.1")
				member("TestMbr1.2.2")
			}
			member("TestMbr1.3")
		}
		dimension("TestDim2") {
			member("TestMbr2.1")
			member("TestMbr2.2")
		}
		dimension("TestDim3") {
			member("TestMbr3.1")
		}
	}
}

(i, k) = [0, 0]

IEssbase.withServer(signOn) { essSvr ->
	essSvr.withApplication('zTstApp') { essApp ->
		try {
			essApp.withOutline('zTstCube') { essOtl ->
				essOtl.eachMember { essMbr ->
					assert essMbr instanceof com.essbase.api.metadata.IEssMember
					if (essMbr.name == 'TestMbr1.2') {
						assert essMbr.relatedMemberNames == ["TestDim1", "TestMbr1.1", "TestMbr1.3", "TestMbr1.2.1"]
						++i
					}
					if (essMbr.name == 'TestDim2') {
						assert essMbr.relatedMemberNames == ["", "TestDim1", "TestDim3", "TestMbr2.1"]
						++k
					}
				}
			}
		} finally {
			essApp.delete()
		}
	}
}

assert i > 0
assert k > 0
