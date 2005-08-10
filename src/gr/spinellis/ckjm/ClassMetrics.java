/*
 * $Id: \\dds\\src\\Research\\ckjm.RCS\\src\\gr\\spinellis\\ckjm\\ClassMetrics.java,v 1.9 2005/08/10 16:42:28 dds Exp $
 *
 * (C) Copyright 2005 Diomidis Spinellis
 *
 * Permission to use, copy, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package gr.spinellis.ckjm;

import java.util.HashSet;

/**
 * Store details needed for calculating a class's Chidamber-Kemerer metrics.
 * Most fields in this class are set by ClassVisitor.
 * This class also encapsulates some policy decision regarding metrics
 * measurement.
 *
 * @see ClassVisitor
 * @version $Revision: 1.9 $
 * @author <a href="http://www.spinellis.gr">Diomidis Spinellis</a>
 */
public class ClassMetrics {
    /** Weighted methods per class */
    private int wmc;
    /** Number of children */
    private int noc;
    /** Response for a Class */
    private int rfc;
    /** The class's parent class */
    private ClassMetrics parent;
    /** Coupling between object classes */
    private int cbo;
    /** Lack of cohesion in methods */
    private int lcom;
    /** Number of public methods */
    private int npm;
    /** True if the class has been visited by the metrics gatherer */
    private boolean visited;
    /** True if the class is public */
    private boolean isPublic;
    /** Coupled classes: classes that use this class */
    private HashSet<String> efferentCoupledClasses;

    /** Default constructor. */
    ClassMetrics() {
	wmc = 0;
	noc = 0;
	cbo = 0;
	npm = 0;
	parent = null;
	visited = false;
	efferentCoupledClasses = new HashSet<String>();
    }

    /** Increment the weighted methods count */
    public void incWmc() { wmc++; }
    /** Return the weighted methods per class metric */
    public int getWmc() { return wmc; }

    /** Increment the number of children */
    public void incNoc() { noc++; }
    /** Return the number of children */
    public int getNoc() { return noc; }

    /** Increment the Response for a Class */
    public void setRfc(int r) { rfc = r; }
    /** Return the Response for a Class */
    public int getRfc() { return rfc; }

    /* Set the class's parent */
    public void setParent(ClassMetrics p) { parent = p; }
    /** Return the class's parent */
    public ClassMetrics getParent() { return parent; }
    /** Return the depth of the class's inheritance tree */
    public int getDit() {
	int i = 0;
	ClassMetrics c = parent;
	while (c != null) {
	    c = c.getParent();
	    i++;
	}
	return i;
    }

    /** Increment the coupling between object classes metric */
    public void setCbo(int c) { cbo = c; }
    /** Return the coupling between object classes metric */
    public int getCbo() { return cbo; }

    /** Return the class's lack of cohesion in methods metric */
    public int getLcom() { return lcom; }
    /** Set the class's lack of cohesion in methods metric */
    public void setLcom(int l) { lcom = l; }

    /** Return the class's efferent couplings metric */
    public int getCe() { return efferentCoupledClasses.size(); }
    /** Add a class to the set of classes that depend on this class */
    public void addEfferentCoupling(String name) { efferentCoupledClasses.add(name); }

    /** Increment the number of public methods count */
    public void incNpm() { npm++; }
    /** Return the number of public methods metric */
    public int getNpm() { return npm; }
    /** Return true if the class name is part of the Java SDK */

    public static boolean isJdkClass(String s) {
	return (s.startsWith("java.") ||
		s.startsWith("javax.") ||
		s.startsWith("org.omg.") ||
		s.startsWith("org.w3c.dom.") ||
		s.startsWith("org.xml.sax."));
    }

    /** Return the 6 CK metrics plus Ce as a space-separated string */
    public String toString() {
	return (
		wmc +
		" " + getDit() +
		" " + noc +
		" " + cbo +
		" " + rfc +
		" " + lcom +
		" " + getCe()+
		" " + npm);
    }

    /** Mark the instance as visited by the metrics analyzer */
    public void setVisited() { visited = true; }
    /**
     * Return true if the class has been visited by the metrics analyzer.
     * Classes may appear in the collection as a result of some kind
     * of coupling.  However, unless they are visited an analyzed,
     * we do not want them to appear in the output results.
     */
    public boolean isVisited() { return visited; }
}
