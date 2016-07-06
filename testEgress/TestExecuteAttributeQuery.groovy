import com.essbase.api.metadata.IEssAttributeQuery

run './ConfigTests.groovy' as File // initializes signOn variable

com.hyperionaddict.egress.Egress.withServer(signOn) { essSvr ->
    essSvr.withCube('Sample', 'Basic') { essCube ->

        // Asking for all the attribute dimensions associated with a base dimension.
        def results = essCube.executeAttributeQuery { essQry ->
            essQry.set(IEssAttributeQuery.MBR_TYPE_BASE_DIMENSION, IEssAttributeQuery.MBR_TYPE_ATTRIBUTE_DIMENSION)
            essQry.setInputMember('Product') // Any member of the base dimension will do.
        }
        assert results*.name == ['Caffeinated', 'Ounces', 'Pkg Type', 'Intro Date']

        // Asking for the base dimension associated with an attribute dimension.
        results = essCube.executeAttributeQuery { essQry ->
            essQry.set(IEssAttributeQuery.MBR_TYPE_ATTRIBUTE_DIMENSION, IEssAttributeQuery.MBR_TYPE_BASE_DIMENSION)
            essQry.setInputMember('Population') // Any member of the attribute dimension will do.
        }
        assert results*.name == ['Market']

        // Asking for all the attribute members associated with a base member.
        results = essCube.executeAttributeQuery { essQry ->
            essQry.set(IEssAttributeQuery.MBR_TYPE_BASE_MEMBER, IEssAttributeQuery.MBR_TYPE_ATTRIBUTE_MEMBER)
            essQry.setInputMember('100-10')
        }
        assert results*.name == ['Caffeinated_True', 'Ounces_12', 'Can', 'Intro Date_03-25-1996']

        // Asking for the attribute members from a given attribute dimension associated with a base member.
        results = essCube.executeAttributeQuery { IEssAttributeQuery essQry ->
            essQry.set(IEssAttributeQuery.MBR_TYPE_BASE_MEMBER, IEssAttributeQuery.MBR_TYPE_ATTRIBUTE_MEMBER)
            essQry.setInputMember('100-10')
            essQry.setAttributeValue(IEssAttributeQuery.OP_EQ, 'Caffeinated') // Any member of the attrib dim will do here. Filters the results down to this attrib dimension.
        }
        assert results*.name == ['Caffeinated_True']

        // Asking for the attribute members from a given dimension associated with a base member.
        results = essCube.executeAttributeQuery { IEssAttributeQuery essQry ->
            essQry.set(IEssAttributeQuery.MBR_TYPE_ATTRIBUTE_MEMBER, IEssAttributeQuery.MBR_TYPE_BASE_MEMBER)
            essQry.setInputMember('Can')
        }
        assert results*.name == ['100-10', '100-20', '300-30']

        // Asking for the attribute members from a given dimension associated with a base member, but doing it wrong.
        results = essCube.executeAttributeQuery { IEssAttributeQuery essQry ->
            essQry.set(IEssAttributeQuery.MBR_TYPE_ATTRIBUTE_MEMBER, IEssAttributeQuery.MBR_TYPE_BASE_MEMBER)
            essQry.setInputMember('Can')
            essQry.setAttributeValue(IEssAttributeQuery.OP_EQ, 'Pkg Type') // Don't do this. It can only mess you up. Only call this when querying _FOR_ attribute members.
        }
        assert results*.name == []

        /* I don't know how to use any of the other ones. "ATTRIBUTED"? What's that?
           And STANDARD_MEMBER is not the same as BASE_MEMBER. IT doesn't seem to work at all. */
    }

}
