import com.essbase.api.metadata.IEssDimension

run './ConfigTests.groovy' as File // initializes signOn variable

def i = 0

com.hyperionaddict.egress.Egress.withServer(signOn) { essSvr ->
    essSvr.withOutline('Sample', 'Basic') { essOtl ->
        essOtl.eachDimension { IEssDimension essMbr ->
            assert essMbr instanceof com.essbase.api.metadata.IEssDimension
            ++i
        }
    }
}

assert i > 0
