run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.withServer(signOn) { essSvr ->
	essSvr.withOutline('Sample', 'Basic') { essOtl ->

		/* Test eachDescendant() */
		def results = []
		essOtl.findMember('Year').eachDescendant { results << it.name }
		assert results == ['Jan', 'Feb', 'Mar', 'Qtr1', 'Apr', 'May', 'Jun', 'Qtr2', 'Jul', 'Aug', 'Sep', 'Qtr3', 'Oct', 'Nov', 'Dec', 'Qtr4']

		results = []
		essOtl.findMember('Year').eachDescendant(true) { results << it.name }
		assert results == ['Jan', 'Feb', 'Mar', 'Qtr1', 'Apr', 'May', 'Jun', 'Qtr2', 'Jul', 'Aug', 'Sep', 'Qtr3', 'Oct', 'Nov', 'Dec', 'Qtr4', 'Year']

		results = []
		essOtl.findMember('100').eachDescendant { results << it.name }
		assert results == ['100-10', '100-20', '100-30']

		results = []
		essOtl.findMember('100').eachDescendant(true) { results << it.name }
		assert results == ['100-10', '100-20', '100-30', '100']

		results = []
		essOtl.findMember('New York').eachDescendant { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('New York').eachDescendant(true) { results << it.name }
		assert results == ['New York']

		/* Test eachDescendantAtLevel() */
		results = []
		essOtl.findMember('Year').eachDescendantAtLevel(0) { results << it.name }
		assert results == ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

		results = []
		essOtl.findMember('Year').eachDescendantAtLevel(1) { results << it.name }
		assert results == ['Qtr1', 'Qtr2', 'Qtr3', 'Qtr4']

		results = []
		essOtl.findMember('Year').eachDescendantAtLevel(2) { results << it.name }
		assert results == ['Year']

		results = []
		essOtl.findMember('Year').eachDescendantAtLevel(3) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('100').eachDescendantAtLevel(0) { results << it.name }
		assert results == ['100-10', '100-20', '100-30']

		results = []
		essOtl.findMember('100').eachDescendantAtLevel(1) { results << it.name }
		assert results == ['100']

		results = []
		essOtl.findMember('100').eachDescendantAtLevel(2) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('New York').eachDescendantAtLevel(0) { results << it.name }
		assert results == ['New York']

		results = []
		essOtl.findMember('New York').eachDescendantAtLevel(1) { results << it.name }
		assert results == []


		/* Test eachDescendantAtGeneration() */
		results = []
		essOtl.findMember('Year').eachDescendantAtGeneration(3) { results << it.name }
		assert results == ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

		results = []
		essOtl.findMember('Year').eachDescendantAtGeneration(2) { results << it.name }
		assert results == ['Qtr1', 'Qtr2', 'Qtr3', 'Qtr4']

		results = []
		essOtl.findMember('Year').eachDescendantAtGeneration(1) { results << it.name }
		assert results == ['Year']

		results = []
		essOtl.findMember('Year').eachDescendantAtGeneration(0) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('Year').eachDescendantAtGeneration(4) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('100').eachDescendantAtGeneration(3) { results << it.name }
		assert results == ['100-10', '100-20', '100-30']

		results = []
		essOtl.findMember('100').eachDescendantAtGeneration(2) { results << it.name }
		assert results == ['100']

		results = []
		essOtl.findMember('100').eachDescendantAtGeneration(1) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('100').eachDescendantAtGeneration(4) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('New York').eachDescendantAtGeneration(3) { results << it.name }
		assert results == ['New York']

		results = []
		essOtl.findMember('New York').eachDescendantAtGeneration(2) { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('New York').eachDescendantAtGeneration(4) { results << it.name }
		assert results == []


		/* Test eachChild() */
		results = []
		essOtl.findMember('Year').eachChild { results << it.name }
		assert results == ['Qtr1', 'Qtr2', 'Qtr3', 'Qtr4']

		results = []
		essOtl.findMember('Year').eachChild(true) { results << it.name }
		assert results == ['Qtr1', 'Qtr2', 'Qtr3', 'Qtr4', 'Year']

		results = []
		essOtl.findMember('100').eachChild { results << it.name }
		assert results == ['100-10', '100-20', '100-30']

		results = []
		essOtl.findMember('100').eachChild(true) { results << it.name }
		assert results == ['100-10', '100-20', '100-30', '100']

		results = []
		essOtl.findMember('New York').eachChild { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('New York').eachChild(true) { results << it.name }
		assert results == ['New York']


		/* Test eachAncestor() */
		results = []
		essOtl.findMember('Year').eachAncestor { results << it.name }
		assert results == []

		results = []
		essOtl.findMember('Year').eachAncestor(true) { results << it.name }
		assert results == ['Year']

		results = []
		essOtl.findMember('100').eachAncestor { results << it.name }
		assert results == ['Product']

		results = []
		essOtl.findMember('100').eachAncestor(true) { results << it.name }
		assert results == ['100', 'Product']

		results = []
		essOtl.findMember('New York').eachAncestor { results << it.name }
		assert results == ['East', 'Market']

		results = []
		essOtl.findMember('New York').eachAncestor(true) { results << it.name }
		assert results == ['New York', 'East', 'Market']

		/* Test shared members */
		essOtl.findMember('Diet').eachChild { childMbr ->
			results = []
			childMbr.eachAncestor { results << it.name }
			assert results == ['Diet', 'Product']
		}



	}
}
