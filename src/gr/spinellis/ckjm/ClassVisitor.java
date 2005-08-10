/*
 * $Id: \\dds\\src\\Research\\ckjm.RCS\\src\\gr\\spinellis\\ckjm\\ClassVisitor.java,v 1.14 2005/08/10 16:42:28 dds Exp $
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

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Repository;
import org.apache.bcel.Constants;
import org.apache.bcel.util.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Modifier;

/**
 * Visit a class updating its Chidamber-Kemerer metrics.
 *
 * @see ClassMetrics
 * @version $Revision: 1.14 $
 * @author <a href="http://www.spinellis.gr">Diomidis Spinellis</a>
 */
public class ClassVisitor extends org.apache.bcel.classfile.EmptyVisitor {
    /** The class being visited. */
    private JavaClass visitedClass;
    /** The class's constant pool. */
    private ConstantPoolGen cp;
    /** The class's fully qualified name. */
    private String myClassName;
    /** The container where metrics for all classes are stored. */
    private ClassMetricsContainer cmap;
    /** The emtrics for the class being visited. */
    private ClassMetrics cm;
    /* Classes encountered.
     * Its cardinality is used for calculating the CBO.
     */
    private HashSet<String> afferentCoupledClasses = new HashSet<String>();
    /** Methods encountered.
     * Its cardinality is used for calculating the RFC.
     */
    private HashSet<String> responseSet = new HashSet<String>();
    /** Use of fields in methods.
     * Its contents are used for calculating the LCOM.
     * We use a Tree rather than a Hash to calculate the
     * intersection in O(n) instead of O(n*n).
     */
    ArrayList<TreeSet<String>> mi = new ArrayList<TreeSet<String>>();

    public ClassVisitor(JavaClass jc, ClassMetricsContainer classMap) {
	visitedClass = jc;
	cp = new ConstantPoolGen(visitedClass.getConstantPool());
	cmap = classMap;
	myClassName = jc.getClassName();
	cm = cmap.getMetrics(myClassName);
    }

    /** Return the class's metrics container. */
    public ClassMetrics getMetrics() { return cm; }

    public void start() {
	visitJavaClass(visitedClass);
    }

    /** Calculate the class's metrics based on its elements. */
    public void visitJavaClass(JavaClass jc) {
	String super_name   = jc.getSuperclassName();
	String package_name = jc.getPackageName();

	cm.setVisited();
	ClassMetrics pm = cmap.getMetrics(super_name);

	pm.incNoc();
	/*
	 * JRE's Object has Object as its parent.
	 * Skip it or else we'll get into infinite recursion
	 * when calculating DIT.
	 */
	if (!jc.getClassName().equals("java.lang.Object"))
	    cm.setParent(pm);
	registerCoupling(super_name);

	String ifs[] = jc.getInterfaceNames();
	/* Measuring decision: couple interfaces */
	for (int i = 0; i < ifs.length; i++)
	    registerCoupling(ifs[i]);

	Field[] fields = jc.getFields();
	for(int i=0; i < fields.length; i++)
	    fields[i].accept(this);

	Method[] methods = jc.getMethods();
	for(int i=0; i < methods.length; i++)
	    methods[i].accept(this);
    }

    /** Add a given class to the classes we are coupled to */
    public void registerCoupling(String className) {
	/* Measuring decision: don't couple to Java SDK */
	if ((MetricsFilter.isJdkIncluded() ||
	     !ClassMetrics.isJdkClass(className)) &&
	    !myClassName.equals(className)) {
	    afferentCoupledClasses.add(className);
	    cmap.getMetrics(className).addEfferentCoupling(myClassName);
	}
    }

    /* Add the type's class to the classes we are coupled to */
    public void registerCoupling(Type t) {
	registerCoupling(className(t));
    }

    /* Add a given class to the classes we are coupled to */
    void registerFieldAccess(String className, String fieldName) {
	registerCoupling(className);
	if (className.equals(myClassName))
	    mi.get(mi.size() - 1).add(fieldName);
    }

    /* Add a given method to our response set */
    void registerMethodInvocation(String className, String methodName) {
	registerCoupling(className);
	/* Measuring decision: calls to JDK methods are included in the RFC calculation */
	responseSet.add(className + "." + methodName);
    }

    /** Called when a field access is encountered. */
    public void visitField(Field field) {
	registerCoupling(field.getType());
    }

    /** Called when a method invocation is encountered. */
    public void visitMethod(Method method) {
	/* Measuring decision: A class's own methods contribute to its RFC */
	responseSet.add(visitedClass.getClassName() + "." + method.getName());
	MethodGen mg = new MethodGen(method, visitedClass.getClassName(), cp);

	Type   result_type = mg.getReturnType();
	Type[] arg_types   = mg.getArgumentTypes();

	registerCoupling(mg.getReturnType());
	for (int i = 0; i < arg_types.length; i++)
	    registerCoupling(arg_types[i]);
	cm.incWmc();
	if (Modifier.isPublic(method.getModifiers()))
		cm.incNpm();
	mi.add(new TreeSet<String>());
	MethodVisitor factory = new MethodVisitor(mg, this);
	factory.start();
    }

    /** Return a class name associated with a type. */
    static String className(Type t) {
	String ts = t.toString();

	if (t.getType() <= Constants.T_VOID) {
	    return "java.PRIMITIVE";
	} else if(t instanceof ArrayType) {
	    ArrayType at = (ArrayType)t;
	    return className(at.getBasicType());
	} else {
	    return t.toString();
	}
    }

    /** Do final accounting at the end of the visit. */
    public void end() {
	cm.setCbo(afferentCoupledClasses.size());
	cm.setRfc(responseSet.size());
	/*
	 * Calculate LCOM  as |P| - |Q| if |P| - |Q| > 0 or 0 otherwise
	 * where
	 * P = set of all empty set intersections
	 * Q = set of all nonempty set intersections
	 */
	int lcom = 0;
	for (int i = 0; i < mi.size(); i++)
	    for (int j = i + 1; j < mi.size(); j++) {
		/* A shallow unknown-type copy is enough */
		TreeSet<?> intersection = (TreeSet<?>)mi.get(i).clone();
		intersection.retainAll(mi.get(j));
		if (intersection.size() == 0)
		    lcom++;
		else
		    lcom--;
	    }
	cm.setLcom(lcom > 0 ? lcom : 0);
    }
}
