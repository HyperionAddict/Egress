package com.hyperionaddict.egress

import com.essbase.api.session.IEssbase
import com.essbase.api.datasource.IEssOlapServer
import com.essbase.api.datasource.IEssOlapApplication
import com.essbase.api.datasource.IEssOlapApplication.EEssDataStorageType
import com.essbase.api.datasource.IEssCube
import com.essbase.api.datasource.IEssCube.EEssRestructureOption
import com.essbase.api.datasource.IEssOlapFileObject
import com.essbase.api.metadata.IEssCubeOutline
import com.essbase.api.metadata.IEssDimension
import com.essbase.api.metadata.IEssMember
import com.essbase.api.metadata.IEssMember.EEssShareOption

/**
 * Created by IntelliJ IDEA.
 * User: jwaultman
 * Date: 6/6/11
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
 */
class OutlineBuilder extends BuilderSupport {

	class OutlineBuilderException extends RuntimeException { // This is important because the builder will sometimes eat exceptions and re-call a method.
		OutlineBuilderException(Throwable t) {           // Builders need to raise exceptions that are not used by the general runtime.
			super(t)                                 // Here, we create a whole new exception type that only we know about. Problem solved.
		}
	}

	EssSignOn signOn // This must be set before any building methods are called, so we can log on and get started with building when we are ready.
	String server // This must be set before any building methods are called, so we can log on and get started with building when we are ready.
	String application // This can be set as an alternative to starting with an 'application' node.
	String cube // This can be set as an alternative to starting with an 'application' or 'cube' node. The application field will need to be set, too.
	String dimension
	String member

	private validMethods = ['application', 'cube', 'dimension', 'member']

	private IEssbase essHome
	private IEssOlapServer essSvr
	private IEssOlapApplication essApp
	private IEssCube essCube
	private IEssCubeOutline essOtl
	private IEssDimension essDim
	private IEssMember essDimMbr
	private IEssMember essMbr

	private IEssMember curParent
	private IEssMember prevSibling
	private mbrTrail = [] as Queue
	private startDepth = 0

	boolean isActive = false
	int depth = 0

	Object createNode(Object name) {
		return createNode(name, null, null)
	}

	Object createNode(Object name, Object value) {
		return createNode(name, null, value)
	}

	Object createNode(Object name, Map attributes) {
		return createNode(name, attributes, null)
	}

	Object createNode(Object name, Map attributes, Object value) {
		//println "${'\t'*depth}Create node(name, attr, val): ${name.toString()},  ${attributes.toString()}, ${value.toString()}"

		try {
			if (!value) {
				throw new OutlineBuilderException("No value provided for member name.")
			} else if (! validMethods.contains(name)) {
				throw new OutlineBuilderException("Invalid method call: ${name}. Only the methods [${validMethods.inject(''){r,a->r<<"'"<<a<<"' "}}] may be called on the OutlineBuilder.")
			}
			if (!isActive) {
				this.activate()
			}
			def parent
			def prevSib
			def retval
			switch (name) {
				case 'application':
					application = value
					essApp = essSvr.getApplicationOrNull(value)
					if (!essApp) {
						if (attributes?.storageType == EEssDataStorageType.ASO) {
							essApp = essSvr.createApplication(value, 4 as short, attributes?.appType ?: 'native')
						} else {
							essApp = essSvr.createApplication(value, attributes?.appType ?: 'native')
						}
					}
					retval = essApp
					break
				case 'cube':
					cube = value
					essCube = essApp.getCubeOrNull(value)
					if (!essCube) {
						essCube = essApp.createCube(value, attributes?.cubeType ?: IEssCube.EEssCubeType.NORMAL, attributes?.allowNonUniqNames ?: false)
					}
					essOtl = essCube.openOutline(false, true, true)
					retval = essCube
					break
				case 'dimension':
					dimension = value
					startDepth = startDepth ?: depth
					(mbrTrail.size() - (depth - startDepth /*the effective depth*/)).times {
						prevSib = mbrTrail.pollLast() // Pop members off the stack to match the current depth, and the last one off is the prevSib we need.
					}
					essDim = essOtl.findDimensionOrNull(value)
					if (!essDim) {
						essDim = essOtl.createDimension(value, prevSib)
					}
					essDimMbr = essDim.dimensionRootMember
					mbrTrail.addLast(essDim)
					retval = essDimMbr
					break
				case 'member':
					member = value
					startDepth = startDepth ?: depth
					(mbrTrail.size() - (depth - startDepth /*the effective depth*/)).times {
						prevSib = mbrTrail.pollLast() // Pop members off the stack to match the current depth, and the last one off is the prevSib we need.
					}
					parent = mbrTrail.peekLast() // The parent we need is at the top of the stack but we want to leave it there.
					parent = (parent instanceof IEssDimension) ? parent.dimensionRootMember : parent
					assert parent instanceof IEssMember
					if (attributes?.shareOption == EEssShareOption.SHARED_MEMBER) {
						essMbr = parent.createChildMember(value, prevSib, EEssShareOption.SHARED_MEMBER)
					} else {
						essMbr = essOtl.findMemberOrNull(value)
						if (!essMbr) {
							essMbr = parent.createChildMember(value, prevSib, attributes?.shareOption ?: EEssShareOption.STORE_DATA)
						}
						attributes.each { k, v ->
							essMbr[k] = v // so Groovy!
						}
						essMbr.updatePropertyValues()
					}
					mbrTrail.addLast(essMbr)
					retval = essMbr
					break
				default:
					assert false, "Bad method call to OutlineBuilder ${name} not caught by upfront checks" // should never happen
			}
			++depth
			return retval
		} catch (Exception e) {
			this.deactivate()
			throw new OutlineBuilderException(e)
		}
	}

	void setParent (Object parent, Object child) {
		//println "${'\t'*depth}Set parent(par, chld): ${parent.toString()}, ${child.toString() ?: child.name}"
//		try {
//			if (parent instanceof IEssMember ) {
//				def myParent = essOtl.refreshMember(parent)
//				switch (myParent.childCount) {
//					case 0:
//						essOtl.moveMember(child, myParent, null)
//						break
//					case 1:
//						if (child.relatedMemberNames[0] != myParent.name) {
//							essOtl.moveMember(child, null, myParent.childMembers[0])
//						}
//						break
//					default:
//						def children = myParent.childMembers
//						if (child.name != children[-1].name) {
//							essOtl.moveMember(child, null, children[-1])
//						}
//						break
//				}
//			}
//		} catch (Exception e) {
//			this.deactivate()
//			throw new OutlineBuilderException(e)
//		}
//		try {
//			if (parent instanceof IEssMember ) {
//				essOtl.moveMember(child, null, prevSibling)
//			}
//		} catch (Exception e) {
//			this.deactivate()
//			throw new OutlineBuilderException(e)
//		}
	}

	void nodeCompleted(Object parent, Object node) {
		--depth
		if (depth == startDepth - 1) {
			mbrTrail.size().times {
				mbrTrail.removeLast()
			}
			startDepth = 0
		}
		//println "${'\t'*depth}Node completed(par, node): ${parent.toString()}, ${node.toString() ?: child.name}"
		if (depth == 0) {
			this.deactivate()
		}
	}

	private activate() {
		try {
			if (!signOn) {
				throw new OutlineBuilderException("Unable to sign on. OutlineBuilder can not create nodes until an EssSignOn class has been assigned to its signOn property.")
			}
			com.hyperionaddict.egress.Egress.expandJapi()
			essHome = IEssbase.Home.create(IEssbase.JAPI_VERSION)
			essSvr = signOn?.execute(essHome)
			isActive = true
		} catch (Throwable e) {
			this.deactivate()
			throw new OutlineBuilderException(e)
		}

	}

	private deactivate() {
		// This is all the cleanup code. It should be called whenever the root node is completed, or when something goes wrong
		// and we need to quit building.
		// This code has a lot of try/catch blocks that eat exceptions and just log the problems. We are quitting anyway, so
		// that's OK, I guess. We want as much of the cleanup to succeed as we can manage.
		// Further, in some cases, this method is being called just prior to thowing some other exception that has already occurred.
		// It is better to throw that one, ignoring exceptions here.

		essMbr = null

		essDimMbr = null

		essDim = null

		try {
			essOtl?.save(EEssRestructureOption.KEEP_ALL_DATA)
		} catch (Exception e) {
			println "Outline save failed."
		}
		try {
			essOtl?.cube?.application?.olapServer?.with {
				def cn = essOtl.cube.name
				def an = essOtl.cube.application.name
				if (getOlapFileObject(an, cn, IEssOlapFileObject.TYPE_OUTLINE, cn).isLocked()) {
					unlockOlapFileObject(an, cn, IEssOlapFileObject.TYPE_OUTLINE, cn)
				}
			}
		} catch (Exception e) {
			println "Outline unlock failed."
		}
		essOtl = null

		try {
			essCube?.clearActive()
		} catch (Exception e) {
			println "Cube clearActive() failed."
		}
		essCube = null

		essApp = null

		if (essSvr?.connected) {
			try {
				essSvr.disconnect()
			} catch (Exception e) {
				println "Server disconnect failed."
			}
		}
		essSvr = null

		if (essHome?.signedOn) {
			try {
				essHome.signOff()
			} catch (Exception e) {
				println "Home signOff failed."
			}
		}
		essHome = null

		isActive = false
	}

}
