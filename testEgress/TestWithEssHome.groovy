import com.essbase.api.session.IEssbase

def (i, j) = [0, 0]

com.hyperionaddict.egress.Egress.withHome { essHome ->
	assert essHome instanceof com.essbase.api.session.IEssbase
	++i
}

// This works because the previous withHome() call implicitly calls
// expandJapi() in the Egress class, which adds withHome() to the IEssbase interface.
IEssbase.withHome { essHome ->
	assert essHome instanceof com.essbase.api.session.IEssbase
	++j
}

assert i > 0 && j > 0
