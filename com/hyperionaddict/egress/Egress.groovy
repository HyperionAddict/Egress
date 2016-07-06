package com.hyperionaddict.egress

import com.essbase.api.base.EssException
import com.essbase.api.base.IEssIterator
import com.essbase.api.datasource.IEssCube
import com.essbase.api.datasource.IEssOlapApplication
import com.essbase.api.datasource.IEssOlapFileObject
import com.essbase.api.datasource.IEssOlapServer
import com.essbase.api.metadata.IEssAttributeQuery
import com.essbase.api.metadata.IEssCubeOutline
import com.essbase.api.metadata.IEssMember
import com.essbase.api.metadata.IEssMemberSelection
import com.essbase.api.session.IEssbase

/**
 * Created by IntelliJ IDEA.
 * User: jwaultman
 * Date: 6/6/11
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
 */
class Egress {

    private static Boolean isJapiExpanded = false    // We only need to do it once. This flag is set to true at the end of expandJapi().

    /* **********************************************************
       Static methods on Egress class:
       The idea here is to make access to starting points in Essbase
       easy to get to. We want to minimize required imports and required
       knowledge of the JAPI package structure.

       There are no non-static methods in this class.
       Thus, there should be no need to instantiate the class.
       **********************************************************/

    /**
     * A static method to make it easier to get to a resource-safe IEssbase object.
     *
     * Using this method keeps us from having to walk through the JAPI object structure to get an instantiation of IEssbase.
     * <p>
     * This method automatically calls expandJapi()
     *
     * @param c the closure to run within the Home
     */
    static void withHome(Closure c) {
        expandJapi()
        IEssbase.withHome(c) // Call the withHome method that was added to the IEssbase interface by expandJapi().
    }

    /**
     * A static method to make it easier to get to a resource-safe IEssOlapServer object.
     *
     * Using this method keeps us from having to walk through the JAPI object structure to get an instantiation of IEssOlapServer.
     * <p>
     * This method automatically calls expandJapi()
     * <p>
     * This is probably the easiest, best way to dive in and get going with the Egress class.
     * See the TestWithServer test script for an example.
     * <p>
     * The other option is to call expandJapi() explicitly, then use the static methods that it adds to the IEssbase interface.
     *
     * @param signOn the EssSignOn object defining the server to log on to and the credentials to use
     * @param c the closure to run within the server
     */
    static void withServer(EssSignOn signOn, Closure c) {
        expandJapi()
        IEssbase.withServer(signOn, c) // Call the withServer method that was added to the IEssbase interface by expandJapi().
    }

    /**
     * An overload to the EssSignOn version of the withServer() static method, taking a server name, a user name, and a password.
     *
     * This version directly takes a server name, a user name, and a password. It will also activate the default
     * Embedded mode for the JAPI. Probably less safe.
     * <p/>
     * This method has been tested, but is not covered by the Egress test suite. To include it would require hard-coding
     * credentials into the tests.
     *
     * @param server the server to log on to
     * @param user the user name
     * @param password the user's password
     * @param c the closure to run within the server
     */
    static void withServer(String server, String user, String password, Closure c) {
        expandJapi()
        IEssbase.withServer(server, user, password, c) // Call the withServer method that was added to the IEssbase interface by expandJapi().
    }

    static void expandJapi() {

        // Do not do this if it has already been done.
        if (isJapiExpanded) {
            return
        }

        /* **********************************************************
           JAPI Expansion Type One: Injecting Grooviness
           This section is for making changes to the JAPI that give it a more
           Groovy flavor. The idea is to allow more idiomatic Groovy constructs
           anywhere it would feel natural, rather than have workarounds here and
           there throughout the code.
           **********************************************************/

        /**
         * An iterator() method for all classes that implement IEssIterator.
         *
         * Having this method implemented enables all of Groovy's iterator methods (e.g. each, find).
         *
         * @return an iterator that Groovy can use to do its magic.
         */
        IEssIterator.metaClass.iterator = {-> delegate.all.iterator() }

        /**
         * Replacing of the getAt() method in IEssIterator with one that supports negative indexes.
         *
         * @param index
         * @return the item at the given index
         */
        IEssIterator.metaClass.getAt = { int index -> delegate.all.getAt(index) }

        /**
         * Support the range subscript operator in IEssIterator
         *
         * @param range
         * @return a list of the items found at the indices represented by the range
         */
        IEssIterator.metaClass.getAt = { Range range -> delegate.all.getAt(range) }

        /**
         * Support the range subscript operator in IEssIterator, when the range is empty
         *
         * @param range
         * @return an empty list
         */
        IEssIterator.metaClass.getAt = { EmptyRange range -> delegate.all.getAt(range) }

        /**
         * Select a List of items from an IEssIterator using a Collection to identify the indices to be selected.
         *
         * @param indices
         * @return a list of the items found at the indices represented by members of the collection
         */
        IEssIterator.metaClass.getAt = { Collection indices -> delegate.all.getAt(indices) }

        /**
         * Provide the standard Groovy size() method for an IEssIterator.
         * <p>
         * This is common Groovy practice, standardizing around the size() method whenever possible.
         *
         * @return the number of members in the IEssIterator
         */
        IEssIterator.metaClass.size = {-> delegate.count }

        /* **********************************************************
           JAPI Expansion Type Two: Automatic Resource Management
           This section is for adding withXXXX() methods in various places, which take
           a closure argument and are responsible for setting up the needed resources
           prior to running the closure, then tearing down those resources afterward.
           The idea is to allow the user to concentrate on what needs to be done, and
           let Egress worry about the resources.
           As much work as possible should be done via these withXXXX() methods,
           such as by nesting withApplication() and/or withCube() inside a withServer().
           **********************************************************/

        /**
         * A resource-safe alternative to the standard create() method.
         *
         * This method carries out automatic creation and disposal of the IEssbase object without user intervention.
         *
         * @param c the closure to run within the IEssbase object
         */
        IEssbase.metaClass.'static'.withHome = { Closure c ->
            IEssbase essHome
            try {
                essHome = IEssbase.Home.create(IEssbase.JAPI_VERSION)
                c(essHome)
            }
            finally {
                if (essHome?.isSignedOn()) {
                    essHome.signOff()
                }
                essHome = null
            }
        }

        /**
         * An alternative to the standard create() method and the Egress-provided withHome() methods.
         *
         * This method enables going straight to a (resource-safe) IEssOlapServer instance,
         * without explicitly going through a Home or IEssbase object.
         *
         * @param signOn the object defining the server, user, password, and mode of access
         * @param c the closure to run within the server
         */
        IEssbase.metaClass.'static'.withServer = { EssSignOn signOn, Closure c ->
            IEssbase.withHome { essHome ->
                IEssOlapServer essSvr
                try {
                    essSvr = signOn.execute(essHome)
                    c(essSvr)
                }
                finally {
                    if (essSvr?.isConnected()) {
                        essSvr.disconnect()
                    }
                    essSvr = null
                }
            }
        }

        /**
         * An overload to the EssSignOn version of the static withServer(), taking a server name, a user name, and a password.
         *
         * This method also employs the default "Embedded" mode for the JAPI.
         * <p>
         * This version is probably less safe, because the credentials will be more exposed in the calling application.
         * <p/>
         * This method has been tested, but is not covered by the Egress test suite. To include it would require hard-coding
         * credentials into the tests.
         *
         * @param server the server to log on to
         * @param user the user name
         * @param password the user's password
         * @param c the closure to run within the server
         */
        IEssbase.metaClass.'static'.withServer = { String server, String user, String password, Closure c ->
            IEssbase.withServer(new EssSignOn(svr: server, eu: user, ep: password), c)
        }

        /**
         * A resource-safe alternative to the standard getApplication() method.
         *
         * This method carries out automatic creation then disposal of the IEssOlapApplication object without user intervention.
         *
         * @param app the application to open
         * @param c the closure to run within the application
         */
        IEssOlapServer.metaClass.withApplication = { String app, Closure c ->
            IEssOlapApplication essApp
            try {
                essApp = delegate.getApplication(app)
                c(essApp)
            }
            finally { essApp = null }
        }

        /**
         * A resource-safe alternative to the standard getCube() method.
         *
         * This method carries out automatic creation then disposal of the IEssCube object without user intervention.
         *
         * @param cube the cube to open
         * @param c the closure to run within the cube
         */
        IEssOlapApplication.metaClass.withCube = { String cube, Closure c ->
            IEssCube essCube
            try {
                essCube = delegate.getCube(cube)
                c(essCube)
            }
            finally {
                essCube?.clearActive()
                essCube = null
            }
        }

        /**
         * An alternative to the standard getCube() method and the Egress-provided IEssOlapApplication.withCube() method.
         *
         * This method enables going straight to a (resource-safe) IEssCube instance,
         * without explicitly going through an IEssOlapApplication object.
         *
         * @param app the application to open
         * @param cube the cube to open
         * @param c the closure to run within the cube
         */
        IEssOlapServer.metaClass.withCube = { String app, String cube, Closure c ->
            delegate.withApplication(app) { essApp -> essApp.withCube(cube, c) }
        }

        /**
         * A resource-safe alternative to the standard openOutline() method.
         *
         * This method carries out automatic creation then disposal of the IEssCubeOutline object without user intervention.
         * <p>
         * This method allows setting the parameters of the openOutline() call -- readOnly, lock, and keepTrans.
         * This allows more flexibility, including obtaining a read-only outline.
         *
         * @param readOnly the flag to make the outline read-only or mutable
         * @param lock the flag to lock the outline
         * @param keepTrans the flag to activate the transaction log, used for making changes then keeping data on restructure
         * @param c the closure to run within the outline
         */
        IEssCube.metaClass.withOutline = { Boolean readOnly, Boolean lock, Boolean keepTrans, Closure c ->
            IEssCubeOutline essOtl
            try {
                essOtl = delegate.openOutline(readOnly, lock, keepTrans)
                c(essOtl)
            }
            finally {
                if (lock) {
                    essOtl?.cube?.application?.olapServer?.with {
                        def cn = essOtl.cube.name
                        def an = essOtl.cube.application.name
                        if (getOlapFileObject(an, cn, IEssOlapFileObject.TYPE_OUTLINE, cn).isLocked()) {
                            unlockOlapFileObject(an, cn, IEssOlapFileObject.TYPE_OUTLINE, cn)
                        }
                    }
                }
                if (essOtl?.isOpen()) {
                    essOtl.close()
                }
                essOtl = null
            }
        }

        /**
         * A reduced-parameter withOutline(), using defaults.
         *
         * This is an alternative to the Egress-provided IEssCube.withOutline() method.
         * This version uses default values for readOnly, lock, and keepTrans.
         * <p>
         * Use this to get a mutable outline.
         *
         * @param c the closure to run within the outline
         */
        IEssCube.metaClass.withOutline = { Closure c ->
            delegate.withOutline(false, true, true, c)
        }

        /**
         * An alternative to the standard openOutline() method and the Egress-provided IEssCube.withOutline() method.
         *
         * This method enables going straight to a (resource-safe) IEssCubeOutline instance,
         * without explicitly going through an IEssCube object.
         *
         * @param cube the cube to open
         * @param readOnly the flag to make the outline read-only or mutable
         * @param lock the flag to lock the outline
         * @param keepTrans the flag to activate the transaction log, used for making changes then keeping data on restructure
         * @param c the closure to run within the outline
         */
        IEssOlapApplication.metaClass.withOutline = { String cube, Boolean readOnly, Boolean lock, Boolean keepTrans, Closure c ->
            delegate.withCube(cube) { essCube -> essCube.withOutline(readOnly, lock, keepTrans, c) }
        }

        /**
         * A reduced-parameter withOutline(), using default values.
         *
         * This is an alternative to the Egress-provided IEssOlapApplication.withOutline() method.
         * This version uses default values for readOnly, lock, and keepTrans.
         *
         * @param cube the cube to open
         * @param c the closure to run within the outline
         */
        IEssOlapApplication.metaClass.withOutline = { String cube, Closure c ->
            delegate.withCube(cube) { essCube -> essCube.withOutline(c) }
        }

        /**
         * An alternative to the standard openOutline() method and the Egress-provided IEssOlapApplication.withOutline() method.
         *
         * This method enables going straight to a (resource-safe) IEssCubeOutline instance,
         * without explicitly going through an IEssCube or IEssOlapApplication object.
         *
         * @param app the application to open
         * @param cube the cube to open
         * @param readOnly the flag to make the outline read-only or mutable
         * @param lock the flag to lock the outline
         * @param keepTrans the flag to activate the transaction log, used for making changes then keeping data on restructure
         * @param c the closure to run within the outline
         */
        IEssOlapServer.metaClass.withOutline = { String app, String cube, Boolean readOnly, Boolean lock, Boolean keepTrans, Closure c ->
            delegate.withApplication(app) { essApp -> essApp.withCube(cube) { essCube -> essCube.withOutline(readOnly, lock, keepTrans, c) } }
        }

        /**
         * A reduced-parameter withOutline(), using default value.
         *
         * This is an alternative to the Egress-provided IEssOlapServer.withOutline() method.
         * This version uses default values for readOnly, lock, and keepTrans.
         *
         * @param app the application to open
         * @param cube the cube to open
         * @param c the closure to run within the outline
         */
        IEssOlapServer.metaClass.withOutline = { String app, String cube, Closure c ->
            delegate.withApplication(app) { essApp -> essApp.withCube(cube) { essCube -> essCube.withOutline(c) } }
        }

        /**
         * A resource-safe alternative to the standard openMemberSelection() method.
         *
         * This method carries out automatic creation then disposal of the IEssMemberSelection object without user intervention.
         *
         * @param c the closure to run within the IEssMemberSelection
         */
        IEssCube.metaClass.withMemberSelection = { Closure c ->
            IEssMemberSelection essMbrSel
            try {
                essMbrSel = delegate.openMemberSelection('default')
                c(essMbrSel)
            }
            finally {
                essMbrSel?.close() // There is no method to check if it's open first, but no exception is thrown if it's not open when this is called
                essMbrSel = null
            }
        }

        /* **********************************************************
           JAPI Expansion Type Three: Specialized Iterators 
           This section is for adding eachXXXX() methods in various places, which
           take a closure argument and iterate over different sets of objects,
           running the closure within each.
           When possible, these should be used together with the withXXXX() methods.
           **********************************************************/

        /**
         * Iterate through all of the applications on the server in a resource-safe manner.
         *
         * Resource safety is in terms of the applications acquired, not the server object itself, which may
         * have been acquired in any way, or the resources used inside the closure, which may or may not
         * be used in resource-safe ways by the user.
         * <p>
         * Best practice would be to use this method inside of a closure that was sent to withServer()
         * and to further use withXXXX() methods within the closure that this method receives.
         *
         * @param c the closure to run within each application
         */
        IEssOlapServer.metaClass.eachApplication = { Closure c ->
            // Technically, simply each()-ing on the applications property is resource safe, as of now, because of the trivial cleanup.
            // However, client code should not do it, in case resource cleanup becomes necessary on application objects in the future.
            // We force things through the individual withApplication() method for consistency, for future proofness, and because it is not costly.
            // We tolerate the redundancy of having an application, getting its name, then handing the name to a method which turns it back into an application.
            delegate.applications*.name.each { delegate.withApplication(it, c) }
        }

        /**
         * Iterate through all of the cubes in the application in a resource-safe manner.
         *
         * Resource safety is in terms of the cubes acquired, not the application object itself, which may
         * have been acquired in any way, or the resources used inside the closure, which may or may not
         * be used in resource-safe ways by the user.
         * <p>
         * Best practice would be to use this method inside of a closure that was sent to withApplication()
         * and to further use withXXXX() methods within the closure that this method receives.
         *
         * @param c the closure to run within each cube
         */
        IEssOlapApplication.metaClass.eachCube = { Closure c ->
            // Simply each()-ing on the cubes property is not resource safe, because of the non-trivial cleanup.
            // We force things through the individual withApplication() method, even though it causes the
            // redundancy of having a cube, getting its name, then handing the name to a method which turns it back into a cube.
            delegate.cubes*.name.each { delegate.withCube(it, c) }
        }

        /**
         * Iterate through all of the dimensions in the outline.
         *
         * Best practice would be to use this method inside of a closure that was sent to withOutline().
         * See the TestEachDimension test script for an example.
         * <p>
         * This is probably a better option than .getDimensions().each() because the objects that come back
         * from that call are not equivalent to the ones that come back from findDimension().
         * See the notes on findDimensionOrNull().
         *
         * @param c the closure to run within each member
         */
        IEssCubeOutline.metaClass.eachDimension = { Closure c ->
            if (!delegate.open) {
                throw new EssException("Invalid method call on IEssCubeOutline: eachDimension(). Must be called on an open outline.")
            }

            delegate.dimensions*.name.each { dim ->
                def essDim = delegate.findDimension(dim)
                c(essDim)
            }
        }

        /**
         * Iterate through all of the members in the outline.
         *
         * Best practice would be to use this method inside of a closure that was sent to withOutline().
         * See the TestEachMember test script for an example.
         *
         * @param c the closure to run within each member
         */
        IEssCubeOutline.metaClass.eachMember = { Closure c ->
            if (!delegate.open) {
                throw new EssException("Invalid method call on IEssCubeOutline: eachMember(). Must be called on an open outline.")
            }

            //TODO: This algorithm may choke on mega-outlines, due to nested childMembers() calls eating up memory.
            //      We would scale better if we did an explicit, non-recursive walk of the outline, though that
            //      would likely get rather chatty with the server and would need extensive testing to make sure
            //      shared members were properly handled. (The current algorithm handles shared members properly.)

            def walkThroughMember // predefine -- required in order for it to be able to call itself
            walkThroughMember = { IEssMember essMbr ->
                essMbr.childMembers.each { IEssMember childMbr ->
                    walkThroughMember(childMbr)
                }
                c(essMbr)
            }

            delegate.eachDimension { essDim ->
                IEssMember essMbrRoot = essDim.dimensionRootMember
                walkThroughMember(essMbrRoot)
            }
        }

        /**
         * Iterate through all of the members below this member in the outline.
         *
         * @param includeSelf the flag to include this member in the iteration group
         * @param c the closure to run within each member
         */
        IEssMember.metaClass.eachDescendant = { Boolean includeSelf = false, Closure c ->

            //TODO: This algorithm may choke on mega-outlines, due to nested childMembers() calls eating up memory.
            //      We would scale better if we did an explicit, non-recursive walk of the outline, though that
            //      would likely get rather chatty with the server and would need extensive testing to make sure
            //      shared members were properly handled. (The current algorithm handles shared members properly.)

            def self = delegate
            def walkThroughMember // predefine -- required in order for it to be able to call itself
            walkThroughMember = { IEssMember essMbr ->
                essMbr.childMembers.each { IEssMember childMbr ->
                    walkThroughMember(childMbr)
                }
                if (includeSelf || !essMbr.is(self)) {
                    c(essMbr)
                }
            }
            walkThroughMember(self)
        }

        /**
         * Iterate through all of the members below this member in the outline and at a given level.
         *
         * @param target the target level to use for the iteration group
         * @param c the closure to run within each member
         */
        IEssMember.metaClass.eachDescendantAtLevel = { Integer target, Closure c ->

            //TODO: This algorithm may choke on mega-outlines, due to nested childMembers() calls eating up memory.
            //      We would scale better if we did an explicit, non-recursive walk of the outline, though that
            //      would likely get rather chatty with the server and would need extensive testing to make sure
            //      shared members were properly handled. (The current algorithm handles shared members properly.)

            def walkThroughMember // predefine -- required in order for it to be able to call itself
            walkThroughMember = { IEssMember essMbr ->
                //TODO: Think through whether this can be made more efficient by stopping the walk at the target level.
                essMbr.childMembers.each { IEssMember childMbr ->
                    walkThroughMember(childMbr)
                }
                if (essMbr.levelNumber == target) {
                    c(essMbr)
                }
            }

            walkThroughMember(delegate)
        }

        /**
         * Iterate through all of the members below this member in the outline and at a given generation.
         *
         * @param target the target generation to use for the iteration group
         * @param c the closure to run within each member
         */
        IEssMember.metaClass.eachDescendantAtGeneration = { Integer target, Closure c ->

            //TODO: This algorithm may choke on mega-outlines, due to nested childMembers() calls eating up memory.
            //      We would scale better if we did an explicit, non-recursive walk of the outline, though that
            //      would likely get rather chatty with the server and would need extensive testing to make sure
            //      shared members were properly handled. (The current algorithm handles shared members properly.)

            def walkThroughMember // predefine -- required in order for it to be able to call itself
            walkThroughMember = { IEssMember essMbr ->
                //TODO: This can be made more efficient by not walking through children once the target generation is found.
                essMbr.childMembers.each { IEssMember childMbr ->
                    walkThroughMember(childMbr)
                }
                if (essMbr.generationNumber == target) {
                    c(essMbr)
                }
            }

            walkThroughMember(delegate)
        }

        /** Iterate through all of the children of this member.
         *
         * @param includeSelf the flag to include this member in the iteration group
         * @param c the closure to run within each member
         */
        IEssMember.metaClass.eachChild = { Boolean includeSelf = false, Closure c ->
            delegate.childMembers.each { IEssMember childMbr ->
                c(childMbr)
            }
            if (includeSelf) {
                c(delegate)
            }
        }

        /** Iterate through all of the ancestors of this member.
         *
         * @param includeSelf the flag to include this member in the iteration group
         * @param c the closure to run within each member
         */
        IEssMember.metaClass.eachAncestor = { Boolean includeSelf = false, Closure c ->
            if (includeSelf) {
                c(delegate)
            }
            def curMbr = delegate
            def essOtl = (delegate.shareOption == IEssMember.EEssShareOption.SHARED_MEMBER) ? delegate.parent.parent : delegate.parent // The .parent of a stored member is the outline, the .parent of a shared member is the base member.
            while (curMbr = essOtl.findMemberOrNull(curMbr.relatedMemberNames[0])) {
                c(curMbr)
            }
        }

        /* **********************************************************
           JAPI Expansion Type Four: Tweaks and small enhancements
           This section is for adding methods that make the API
           a little easier to work with.
           **********************************************************/

        /**
         * Set up an attribute query and get back the results.
         *
         * This method makes it easier to work the attribute query workflow and does so in a resource-safe way.
         * In the background, it creates, uses, and destroys the IEssMemberSelection object needed for the query.
         * The user needs to run set(), setInputMember(), and (optionally) setAttributeValue() in the closure.
         * <p>
         * See the Test ExecuteAttributeQuery.groovy test script for examples of use.
         *
         * @param c the closure to run within the IEssAttributeQuery
         * @return the results of the query that is set up in the closure
         */
        IEssCube.metaClass.executeAttributeQuery = { Closure c ->
            delegate.withMemberSelection { essMbrSel ->
                IEssAttributeQuery essAttrQry
                essAttrQry = essMbrSel.createAttributeQuery()
                c(essAttrQry)
                return essMbrSel.queryAttributes(essAttrQry)
            }
        }

        /**
         * A parameterless getAlias() method for all classes that implement IEssMember.
         *
         * Having this version makes getAlias() consistent with all the other getXXXX methods of the interface,
         * all of which work with a parameterless call.
         * <p>
         * This method is equivalent to getAlias(null), i.e. it accesses the default alias table.
         * Further, and more importantly, it allows idiomatic Groovy property-style and square-bracket alias calls to member objects.
         * <p>
         * This means we can do member.alias instead of member.getAlias(null).
         *
         * @return the member's alias from the default alias table
         */
        IEssMember.metaClass.getAlias = {-> delegate.getAlias(null) }

        /**
         * A one-parameter setAlias() method for all classes that implement IEssMember.
         *
         * Having this version makes setAlias() consistent with all the other setXXXX methods of the interface,
         * all of which work with a one-parameter call.
         * <p>
         * This method is equivalent to setAlias(null, 'member alias name'), i.e. it sets to the default alias table.
         * Further, and more importantly, it allows idiomatic Groovy property-style and square-bracket alias assignments to member objects.
         * <p>
         * This means we can do member.alias = 'member alias name' instead of member.setAlias(null, 'member alias name').
         *
         * @param val the value to set the alias to
         */
        IEssMember.metaClass.setAlias = { String val -> delegate.setAlias(null, val) }

        /**
         * A way to circumvent the getApplication() method's exception raising if the application does not exist. Instead returns null.
         *
         * @return the IEssOlapApplication object with the matching name, or null if the application is not found
         */
        IEssOlapServer.metaClass.getApplicationOrNull = { String name ->
            return delegate.applications.find { it.name == name }
        }

        /**
         * A way to circumvent the getCube() method's returning a bogus cube if the cube does not exist. Instead returns null.
         *
         * @return the IEssCube object with the matching name, or null if the cube is not found
         */
        IEssOlapApplication.metaClass.getCubeOrNull = { String name ->
            return delegate.cubes.find { it.name == name }
        }

        /**
         * A way to circumvent the findDimension() method's exception raising if the dimension does not exist. Instead returns null.
         *
         * @return the IEssDimension object with the matching name, or null if the dimension is not found
         */
        IEssCubeOutline.metaClass.findDimensionOrNull = { String name ->
            def essDim = delegate.dimensions.find { it.name == name }  // Yes, this code appears redundant, but for some reason, the IEssDims that come from iterating on getDimensions()
            return essDim ? delegate.findDimension(essDim.name) : null // are not equivalent to the ones from findDimension(). Ex: essDim.dimensionRootMember.createChildMember raises an NPE
        }

        /**
         * A way to circumvent the findMember() method's exception raising if the member does not exist. Instead returns null.
         *
         * @return the IEssMember object with the matching name, or null if the member is not found
         */
        IEssCubeOutline.metaClass.findMemberOrNull = { String name ->
            IEssIterator mayContainMbr = delegate.findMembers([name] as String[]) // Use array parameter override, which does not raise an exception on not found (contradicts documentation).
            return mayContainMbr.count ? mayContainMbr[0] : null
        }

        /**
         * A method that takes a member and re-pulls it from the outline. This allows some properties (childMembers) to be updated.
         *
         * This method is designed to be shared-member aware, so it returns the same member that is fed in, regardless of shared member status.
         *
         * @return the member after it has been re-pulled from the outline and verified to be the same member as before
         */
        IEssCubeOutline.metaClass.refreshMember = { IEssMember essMbr ->
            def tmpMbr = delegate.findMember(essMbr.name)
            def retval = (tmpMbr.relatedMemberNames == essMbr.relatedMemberNames) ? tmpMbr : delegate.getSharedMembers(essMbr.name).find { it.relatedMemberNames == essMbr.relatedMemberNames }
            assert retval, "refreshMember() should always find a member to return"
            return retval
        }

        isJapiExpanded = true

    }

}
