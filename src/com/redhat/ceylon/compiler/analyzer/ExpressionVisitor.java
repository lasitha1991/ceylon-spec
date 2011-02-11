package com.redhat.ceylon.compiler.analyzer;

import com.redhat.ceylon.compiler.model.Class;
import com.redhat.ceylon.compiler.model.ClassOrInterface;
import com.redhat.ceylon.compiler.model.GenericType;
import com.redhat.ceylon.compiler.model.Interface;
import com.redhat.ceylon.compiler.model.Package;
import com.redhat.ceylon.compiler.model.Scope;
import com.redhat.ceylon.compiler.model.Type;
import com.redhat.ceylon.compiler.model.Typed;
import com.redhat.ceylon.compiler.tree.Node;
import com.redhat.ceylon.compiler.tree.Tree;
import com.redhat.ceylon.compiler.tree.Tree.Directive;
import com.redhat.ceylon.compiler.tree.Tree.Expression;
import com.redhat.ceylon.compiler.tree.Tree.MemberOrType;
import com.redhat.ceylon.compiler.tree.Tree.Return;
import com.redhat.ceylon.compiler.tree.Visitor;

/**
 * Third and final phase of type analysis.
 * Finally visit all expressions and determine their types.
 * Use type inference to assign types to declarations with
 * the local modifier. Finally, assigns types to the 
 * associated model objects of declarations declared using
 * the local modifier.
 * 
 * @author Gavin King
 *
 */
public class ExpressionVisitor extends Visitor {
    
    ClassOrInterface classOrInterface;
    
    public void visit(Tree.ClassOrInterfaceDeclaration that) {
        ClassOrInterface o = classOrInterface;
        classOrInterface = (ClassOrInterface) that.getModelNode();
        super.visit(that);
        classOrInterface = o;
    }
    
    //Type inference for members declared "local":
    
    @Override public void visit(Tree.VariableOrExpression that) {
        super.visit(that);
        if (that.getSpecifierExpression()!=null 
                && that.getVariable()!=null
                && (that.getVariable().getTypeOrSubtype() instanceof Tree.LocalModifier)) {
            setType((Tree.LocalModifier) that.getVariable().getTypeOrSubtype(), 
                    that.getSpecifierExpression(), 
                    that.getVariable());
        }
    }
    
    @Override public void visit(Tree.ValueIterator that) {
        super.visit(that);
        if ((that.getVariable().getTypeOrSubtype() instanceof Tree.LocalModifier)) {
            setType((Tree.LocalModifier) that.getVariable().getTypeOrSubtype(), 
                    that.getSpecifierExpression(), 
                    that.getVariable());
        }
    }
    
    @Override public void visit(Tree.KeyValueIterator that) {
        super.visit(that);
        if ((that.getKeyVariable().getTypeOrSubtype() instanceof Tree.LocalModifier)) {
            setType((Tree.LocalModifier) that.getKeyVariable().getTypeOrSubtype(), 
                    that.getSpecifierExpression(), 
                    that.getKeyVariable());
        }
        if ((that.getValueVariable().getTypeOrSubtype() instanceof Tree.LocalModifier)) {
            setType((Tree.LocalModifier) that.getValueVariable().getTypeOrSubtype(), 
                    that.getSpecifierExpression(), 
                    that.getValueVariable());
        }
    }
    
    @Override public void visit(Tree.AttributeDeclaration that) {
        super.visit(that);
        if (that.getTypeOrSubtype() instanceof Tree.LocalModifier) {
            if ( that.getSpecifierOrInitializerExpression()!=null ) {
                setType((Tree.LocalModifier) that.getTypeOrSubtype(), 
                        that.getSpecifierOrInitializerExpression(),
                        that);
            }
            else {
                that.getErrors().add( new AnalysisError(that, 
                        "Could not infer type of: " + 
                        that.getIdentifier().getText()) );
            }
        }
    }

    @Override public void visit(Tree.AttributeGetter that) {
        super.visit(that);
        if (that.getTypeOrSubtype() instanceof Tree.LocalModifier) {
            setType((Tree.LocalModifier) that.getTypeOrSubtype(), 
                    that.getBlock(),
                    that);
        }
    }

    @Override public void visit(Tree.MethodDeclaration that) {
        super.visit(that);
        if (that.getTypeOrSubtype() instanceof Tree.LocalModifier) {
            if (that.getBlock()!=null) {
                setType((Tree.LocalModifier) that.getTypeOrSubtype(), 
                        that.getBlock(),
                        that);
            }
            else if ( that.getSpecifierExpression()!=null ) {
                setType((Tree.LocalModifier) that.getTypeOrSubtype(), 
                        that.getSpecifierExpression(),
                        that);  //TODO: this is hackish
            }
            else {
                that.getErrors().add( new AnalysisError(that, 
                        "Could not infer type of: " + 
                        that.getIdentifier().getText()) );
            }
        }
    }

    private void setType(Tree.LocalModifier local, 
            Tree.SpecifierOrInitializerExpression s, 
            Node that) {
        Type t = s.getExpression().getTypeModel();
        local.setTypeModel(t);
        ((Typed) that.getModelNode()).setType(t);
    }
    
    private void setType(Tree.LocalModifier local, 
            Tree.Block block, Tree.Declaration that) {
        Directive d = block.getDirective();
        if (d!=null && (d instanceof Return)) {
            Type t = ((Return) d).getExpression().getTypeModel();
            local.setTypeModel(t);
            ((Typed) that.getModelNode()).setType(t);
        }
        else {
            local.getErrors().add( new AnalysisError(local, 
                    "Could not infer type of: " + 
                    that.getIdentifier().getText()) );
        }
    }
    
    //Primaries:
    
    @Override public void visit(Tree.MemberExpression that) {
        that.getPrimary().visit(this);
        Type pt = that.getPrimary().getTypeModel();
        if (pt!=null) {
            GenericType gt = pt.getGenericType();
            if (gt instanceof Scope) {
                MemberOrType mt = that.getMemberOrType();
                if (mt instanceof Tree.Member) {
                    Typed member = Util.getDeclaration((Scope) gt, (Tree.Member) mt);
                    that.setTypeModel(member.getType());
                    //TODO: handle type arguments by substitution
                    mt.setModelNode(member);
                }
                else if (mt instanceof Tree.Type) {
                    GenericType member = Util.getDeclaration((Scope) gt, (Tree.Type) mt);
                    Type t = new Type();
                    t.setGenericType(member);
                    t.setTreeNode(that);
                    //TODO: handle type arguments by substitution
                    that.setTypeModel(t);
                    mt.setModelNode(member);
                }
                else if (mt instanceof Tree.Outer) {
                    if (!(gt instanceof ClassOrInterface)) {
                        that.getErrors().add( new AnalysisError(that, 
                                "Can't use outer on a type parameter"));
                    }
                    Type t = getOuterType(mt, (ClassOrInterface) gt);
                    that.setTypeModel(t);
                }
                else {
                    //TODO: handle type parameters by looking at
                    //      their upper bound constraints 
                    //TODO: handle x.outer
                    throw new RuntimeException("Not yet supported");
                }
            }
        }
    }
    
    @Override public void visit(Tree.Annotation that) {
        //TODO: ignore annotations for now
    }
    
    @Override public void visit(Tree.InvocationExpression that) {
        super.visit(that);
        Type pt = that.getPrimary().getTypeModel();
        if (pt!=null) {
            that.setTypeModel(pt); //TODO: this is hackish
        }
        //TODO: validate argument types are assignable to parameter types
    }
    
    @Override public void visit(Tree.IndexExpression that) {
        super.visit(that);
        //TODO!
    }
    
    @Override public void visit(Tree.PostfixOperatorExpression that) {
        super.visit(that);
        Type pt = that.getPrimary().getTypeModel();
        if (pt!=null) {
            that.setTypeModel(pt);
        }
    }
        
    //Atoms:
    
    @Override public void visit(Tree.Member that) {
        //TODO: this does not correctly handle methods
        //      and classes which are not subsequently 
        //      invoked (should return the callable type)
        Type t = Util.getDeclaration(that).getType();
        if (t==null) {
            that.getErrors().add( new AnalysisError(that, 
                    "Could not determine type of member reference: " +
                    that.getIdentifier().getText()) );
        }
        else {
            that.setTypeModel(t);
        }
    }
    
    @Override public void visit(Tree.Type that) {
        //TODO: this does not correctly handle methods
        //      and classes which are not subsequently 
        //      invoked (should return the callable type)
        //that.setType( (Type) that.getModelNode() );
    }
    
    @Override public void visit(Tree.Expression that) {
        //i.e. this is a parenthesized expression
        super.visit(that);
        Type t = that.getTerm().getTypeModel();
        if (t==null) {
            that.getErrors().add( new AnalysisError(that, 
                    "Could not determine type of expression") );
        }
        else {
            that.setTypeModel(t);
        }
    }
    
    @Override public void visit(Tree.Outer that) {
        Type t = getOuterType(that, that.getScope());
        that.setTypeModel(t);
    }

    private Type getOuterType(Node that, Scope scope) {
        Boolean foundInner = false;
        while (!(scope instanceof Package)) {
            if (scope instanceof ClassOrInterface) {
                if (foundInner) {
                    Type t = new Type();
                    t.setGenericType((ClassOrInterface) scope);
                    //TODO: type arguments
                    return t;
                }
                else {
                    foundInner = true;
                }
            }
            scope = scope.getContainer();
        }
        that.getErrors().add( new AnalysisError(that, 
                "Can't use outer outside of nested class or interface"));
        return null;
    }
    
    @Override public void visit(Tree.Super that) {
        if (classOrInterface==null) {
            that.getErrors().add( new AnalysisError(that, 
                    "Can't use super outside a class"));
        }
        else if (!(classOrInterface instanceof Class)) {
            that.getErrors().add( new AnalysisError(that, 
                    "Can't use super inside an interface"));
        }
        else {
            Type t = classOrInterface.getExtendedType();
            //TODO: type arguments
            that.setTypeModel(t);
        }
    }
    
    @Override public void visit(Tree.This that) {
        if (classOrInterface==null) {
            that.getErrors().add( new AnalysisError(that, 
                    "Can't use this outside a class or interface"));
        }
        else {
            Type t = new Type();
            t.setGenericType(classOrInterface);
            //TODO: type arguments
            that.setTypeModel(t);
        }
    }
    
    @Override public void visit(Tree.Subtype that) {
        //TODO!
        throw new RuntimeException();
    }
    
    @Override public void visit(Tree.StringTemplate that) {
        super.visit(that);
        //TODO: validate that the subexpression types are Formattable
        Type t = new Type();
        t.setGenericType( (Class) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.String") );
        that.setTypeModel(t);
    }
    
    @Override public void visit(Tree.SequenceEnumeration that) {
        super.visit(that);
        Type et = null; 
        for (Expression e: that.getExpressionList().getExpressions()) {
            if (et==null) {
                et = e.getTypeModel();
            }
            //TODO: determine the common supertype of all of them
        }
        Type t = new Type();
        t.setGenericType( (Interface) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.Sequence") );
        t.getTypeArguments().add(et);
        that.setTypeModel(t);
    }
    
    @Override public void visit(Tree.StringLiteral that) {
        Type t = new Type();
        t.setGenericType( (Class) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.String") );
        that.setTypeModel(t);
    }
    
    @Override public void visit(Tree.NaturalLiteral that) {
        Type t = new Type();
        t.setGenericType( (Class) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.Natural") );
        that.setTypeModel(t);
    }
    
    @Override public void visit(Tree.FloatLiteral that) {
        Type t = new Type();
        t.setGenericType( (Class) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.Float") );
        that.setTypeModel(t);
    }
    
    @Override public void visit(Tree.CharLiteral that) {
        Type t = new Type();
        t.setGenericType( (Class) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.Character") );
        that.setTypeModel(t);
    }
    
    @Override public void visit(Tree.QuotedLiteral that) {
        Type t = new Type();
        t.setGenericType( (Class) Util.getImportedDeclaration(that.getUnit(), 
                "ceylon.language.Quoted") );
        that.setTypeModel(t);
    }
    
}
