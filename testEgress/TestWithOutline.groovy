import com.essbase.api.session.IEssbase
import com.essbase.api.datasource.IEssOlapFileObject

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def (i, j, k) = [0, 0, 0]

IEssbase.withHome { essHome ->
	essHome.withServer(signOn) { essSvr ->
		essSvr.withApplication('Sample') { essApp ->
			essApp.withCube('Basic') { essCube ->
				essCube.withOutline(true, false, false) {
					assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == false
				}
				essCube.withOutline { essOtl ->
					assert essOtl instanceof com.essbase.api.metadata.IEssCubeOutline
					assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == true
					++i
				}
				assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == false
			}

			essApp.withOutline('Basic', true, false, false) {
				assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == false
			}

			essApp.withOutline('Basic') { essOtl ->
				assert essOtl instanceof com.essbase.api.metadata.IEssCubeOutline
				assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == true
				++j
			}
			assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == false

		}

		essSvr.withOutline('Sample', 'Basic', true, false, false) {
			assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == false
		}

		essSvr.withOutline('Sample', 'Basic') { essOtl ->
			assert essOtl instanceof com.essbase.api.metadata.IEssCubeOutline
			assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == true
			++k
		}
		assert essSvr.getOlapFileObject('Sample', 'Basic', IEssOlapFileObject.TYPE_OUTLINE, 'Basic').isLocked() == false
	}
}

assert i > 0 && j > 0 && k > 0
