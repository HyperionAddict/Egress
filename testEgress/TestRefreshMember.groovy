import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.expandJapi()

def i = 0

// This example shows the problem refreshMember is meant to address, and shows that it succeeds.
// Next, it shows that the method works for shared members as well as base members.
IEssbase.withServer(signOn) { essSvr ->
	essSvr.createApplication('zzRefMbr')
	essSvr.withApplication('zzRefMbr') { essApp ->
		essApp.createCube('zzRefMbr', com.essbase.api.datasource.IEssCube.EEssCubeType.NORMAL)
		essApp.withOutline('zzRefMbr') { essOtl ->
			def essDim = essOtl.createDimension('TestDim')
			def parent = essDim.dimensionRootMember
			def child1 = parent.createChildMember('child1')
			def child2 = parent.createChildMember('child2') // Child2 is inserted as first child.
			assert parent.childMembers*.name == ['child2', 'child1'] // Confirm child2 is before child1.
			essOtl.moveMember(child2, null, child1) // Move child2 to after child1.
			assert parent.childMembers*.name == ['child2', 'child1'] // This should not be the case, but the property is not updated.
			assert essOtl.refreshMember(parent).childMembers*.name == ['child1', 'child2'] // This shows the refresh allows updated info on the property.
			def parent2 = parent.createChildMember('parent2', child2)
			def sharedMbr = parent2.createChildMember('child1', null, com.essbase.api.metadata.IEssMember.EEssShareOption.SHARED_MEMBER)
			def refMbr = essOtl.refreshMember(sharedMbr) // Do a refresh on a shared member.
			assert refMbr.shareOption == sharedMbr.shareOption // Show that we got back the same member as before.
			assert refMbr.relatedMemberNames == sharedMbr.relatedMemberNames // Ditto.
		}
		essApp.delete()
	}
}

// This section demonstrates successful refreshes of every member in Sample.Basic.
IEssbase.withServer(signOn) { essSvr ->
	essSvr.withOutline('Sample', 'Basic') { essOtl ->

		essOtl.eachMember { essMbr ->
			def refMbr = essOtl.refreshMember(essMbr) // Get the member again.
			assert refMbr.shareOption == essMbr.shareOption // Show that it is the same member as before.
			assert refMbr.relatedMemberNames == essMbr.relatedMemberNames // Ditto.
			++i
		}
	}
}

assert i > 0
