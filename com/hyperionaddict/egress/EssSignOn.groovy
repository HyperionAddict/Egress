package com.hyperionaddict.egress

import com.essbase.api.session.IEssbase
import com.essbase.api.datasource.IEssOlapServer

class EssSignOn {
	protected String eu
	protected String ep
	protected String svr
	protected String eas = 'Embedded'

	protected void configure() {}

	final protected IEssOlapServer execute(IEssbase essHome) {
		return	essHome.signOn(eu, ep, false, null, eas, svr)
	}

}
