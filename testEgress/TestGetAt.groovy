run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.withServer(signOn) { essSvr ->
    essSvr.withCube('Sample', 'Basic') { essCube ->
        assert essCube.dimensions[0].name == 'Year'
        assert essCube.dimensions[-2].toString() == 'Intro Date'
        assert essCube.dimensions[0..2]*.name == ['Year', 'Measures', 'Product']
        assert essCube.dimensions[new EmptyRange()] == []
        assert essCube.dimensions[[0, 2, 4]]*.name == ['Year', 'Product', 'Scenario']
    }
}
