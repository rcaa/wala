package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.Atom;

/**
 * We need to generate synthetic methods for Function.apply() in the target
 * selector, so that the AstMethod for functionApply() in the prologue doesn't
 * actually get used in the CGNodes used for calls to Function.apply(). The
 * generated dummy methods should <em>never</em> actually be used except as a
 * stub.
 */
public class JavaScriptFunctionApplyTargetSelector implements MethodTargetSelector {

  private final MethodTargetSelector base;

  private IMethod applyMethod;
  public JavaScriptFunctionApplyTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  private IMethod createApplyDummyMethod(IClass declaringClass) {
    final MethodReference ref = genSyntheticMethodRef(declaringClass);
    // number of args doesn't matter
    JavaScriptSummary S = new JavaScriptSummary(ref, 1);
    return new JavaScriptSummarizedFunction(ref, S, declaringClass);  
  }

  public static final String SYNTHETIC_APPLY_METHOD_PREFIX = "$$ apply_dummy";

  private MethodReference genSyntheticMethodRef(IClass receiver) {
    Atom atom = Atom.findOrCreateUnicodeAtom(SYNTHETIC_APPLY_METHOD_PREFIX);
    Descriptor desc = Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, "()LRoot;");
    MethodReference ref = MethodReference.findOrCreate(receiver.getReference(), atom, desc);
    return ref;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    IMethod method = receiver.getMethod(AstMethodReference.fnSelector);
    if (method != null) {
      String s = method.getReference().getDeclaringClass().getName().toString();
      if (s.equals("Lprologue.js/functionApply")) {
        if (applyMethod == null) {
          applyMethod = createApplyDummyMethod(receiver);
        }
        return applyMethod;
      }
    }
    return base.getCalleeTarget(caller, site, receiver);
  }

}
