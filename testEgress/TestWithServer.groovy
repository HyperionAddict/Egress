import com.essbase.api.session.IEssbase

run './ConfigTests.groovy' as File // initializes signOn variable

def (i, j) = [0, 0]

com.hyperionaddict.egress.Egress.withServer(signOn) { essSvr ->
	assert essSvr instanceof com.essbase.api.datasource.IEssOlapServer
	++i
}

// This works because the previous withServer() call implicitly calls
// expandJapi() in the Egress class, which adds withServer() to the IEssbase interface.
IEssbase.withServer(signOn) { essSvr ->
	assert essSvr instanceof com.essbase.api.datasource.IEssOlapServer
	++j
}

assert i > 0 && j > 0
