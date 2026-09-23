/*
 * 03/21/2010
 *
 * Copyright (C) 2010 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSTALanguageSupport.License.txt file for details.
 */
package com.deckerpw.flateditor.lang.java;

import java.awt.Cursor;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import com.deckerpw.flateditor.lang.ShorthandCompletionCache;
import com.deckerpw.flateditor.lang.java.buildpath.LibraryInfo;
import com.deckerpw.flateditor.lang.java.buildpath.SourceLocation;
import com.deckerpw.flateditor.lang.java.classreader.ClassFile;
import com.deckerpw.flateditor.lang.java.classreader.FieldInfo;
import com.deckerpw.flateditor.lang.java.classreader.MemberInfo;
import com.deckerpw.flateditor.lang.java.classreader.MethodInfo;
import com.deckerpw.flateditor.lang.java.rjc.ast.CodeBlock;
import com.deckerpw.flateditor.lang.java.rjc.ast.CompilationUnit;
import com.deckerpw.flateditor.lang.java.rjc.ast.Field;
import com.deckerpw.flateditor.lang.java.rjc.ast.FormalParameter;
import com.deckerpw.flateditor.lang.java.rjc.ast.ImportDeclaration;
import com.deckerpw.flateditor.lang.java.rjc.ast.LocalVariable;
import com.deckerpw.flateditor.lang.java.rjc.ast.Member;
import com.deckerpw.flateditor.lang.java.rjc.ast.Method;
import com.deckerpw.flateditor.lang.java.rjc.ast.NormalClassDeclaration;
import com.deckerpw.flateditor.lang.java.rjc.ast.TypeDeclaration;
import com.deckerpw.flateditor.lang.java.rjc.lang.Type;
import com.deckerpw.flateditor.lang.java.rjc.lang.TypeArgument;
import com.deckerpw.flateditor.lang.java.rjc.lang.TypeParameter;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;


/**
 * Parses a Java AST for code completions.  It currently scans the following:
 *
 * <ul>
 *    <li>Import statements
 *    <li>Method names
 *    <li>Field names
 * </ul>
 *
 * Also, if the caret is inside a method, local variables up to the caret
 * position are also returned.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class SourceCompletionProvider extends DefaultCompletionProvider {

	private static final Logger LOG =
		System.getLogger(SourceCompletionProvider.class.getName());

	/**
	 * The parent completion provider.
	 */
	private JavaCompletionProvider javaProvider;

	/**
	 * Used to get information about what classes match imports.
	 */
	private JarManager jarManager;

	private static final String JAVA_LANG_PACKAGE			= "java.lang.*";
	private static final String THIS						= "this";

	//Shorthand completions (templates and comments)
	private ShorthandCompletionCache shorthandCache;
	/**
	 * Constructor.
	 */
	SourceCompletionProvider() {
		this(null);
	}


	/**
	 * Constructor.
	 *
	 * @param jarManager The jar manager for this provider.
	 */
	SourceCompletionProvider(JarManager jarManager) {
		if (jarManager==null) {
			jarManager = new JarManager();
		}
		this.jarManager = jarManager;
		setParameterizedCompletionParams('(', ", ", ')');
		setAutoActivationRules(false, "."); // Default - only activate after '.'
		setParameterChoicesProvider(new SourceParamChoicesProvider());
	}


	private void addCompletionsForStaticMembers(Set<Completion> set,
						CompilationUnit cu, ClassFile cf, String pkg) {

		// Check us first, so if we override anything, we get the "newest"
		// version.
		int methodCount = cf.getMethodCount();
		for (int i=0; i<methodCount; i++) {
			MethodInfo info = cf.getMethodInfo(i);
			if (isAccessible(info, pkg) && info.isStatic()) {
				MethodCompletion mc = new MethodCompletion(this, info);
				set.add(mc);
			}
		}

		int fieldCount = cf.getFieldCount();
		for (int i=0; i<fieldCount; i++) {
			FieldInfo info = cf.getFieldInfo(i);
			if (isAccessible(info, pkg) && info.isStatic()) {
				FieldCompletion fc = new FieldCompletion(this, info);
				set.add(fc);
			}
		}

		ClassFile superClass = getClassFileFor(cu, cf.getSuperClassName(true));
		if (superClass!=null) {
			addCompletionsForStaticMembers(set, cu, superClass, pkg);
		}

	}


	/**
	 * Adds completions for accessible methods and fields of super classes.
	 * This is only called when the caret is inside of a class.
	 * TODO: Handle accessibility correctly!
	 *
	 * @param set The set of completions to add to.
	 * @param cu The compilation unit.
	 * @param cf A class in the chain of classes that a type being parsed
	 *        inherits from.
	 * @param pkg The package of the source being parsed.
	 * @param typeParamMap A mapping of type parameters to type arguments
	 *        for the object whose fields/methods/etc. are currently being
	 *        code-completed.
	 */
	private void addCompletionsForExtendedClass(Set<Completion> set,
						CompilationUnit cu, ClassFile cf, String pkg,
						Map<String, String> typeParamMap) {

		// Reset this class's type-arguments-to-type-parameters map, so that
		// when methods and fields need to know type arguments, they can query
		// for them.
		cf.setTypeParamsToTypeArgs(typeParamMap);

		// Check us first, so if we override anything, we get the "newest"
		// version.
		int methodCount = cf.getMethodCount();
		for (int i=0; i<methodCount; i++) {
			MethodInfo info = cf.getMethodInfo(i);
			// Don't show constructors
			if (isAccessible(info, pkg) && !info.isConstructor()) {
				MethodCompletion mc = new MethodCompletion(this, info);
				set.add(mc);
			}
		}

		int fieldCount = cf.getFieldCount();
		for (int i=0; i<fieldCount; i++) {
			FieldInfo info = cf.getFieldInfo(i);
			if (isAccessible(info, pkg)) {
				FieldCompletion fc = new FieldCompletion(this, info);
				set.add(fc);
			}
		}

		// Add completions for any non-overridden super-class methods.
		ClassFile superClass = getClassFileFor(cu, cf.getSuperClassName(true));
		if (superClass!=null) {
			addCompletionsForExtendedClass(set, cu, superClass, pkg, typeParamMap);
		}

		// Add completions for any interface methods, in case this class is
		// abstract and hasn't implemented some of them yet.
		// TODO: Do this only if "top-level" class is declared abstract
		for (int i=0; i<cf.getImplementedInterfaceCount(); i++) {
			String inter = cf.getImplementedInterfaceName(i, true);
			cf = getClassFileFor(cu, inter);
			addCompletionsForExtendedClass(set, cu, cf, pkg, typeParamMap);
		}

	}


	/**
	 * Adds completions for all methods and public fields of a local variable.
	 * This will add nothing if the local variable is a primitive type.
	 *
	 * @param cu The compilation unit being parsed.
	 * @param var The local variable.
	 * @param retVal The set to add completions to.
	 */
	private void addCompletionsForLocalVarsMethods(CompilationUnit cu,
			LocalVariable var, Set<Completion> retVal) {

		Type type = var.getType();
		String pkg = cu.getPackageName();

		if (type.isArray()) {
			ClassFile cf = getClassFileFor(cu, "java.lang.Object");
			addCompletionsForExtendedClass(retVal, cu, cf, pkg, null);
			FieldCompletion fc = FieldCompletion.
				createLengthCompletion(this, type);
			retVal.add(fc);
		}

		else if (!type.isBasicType()) {
			String typeStr = type.getName(true, false);
			ClassFile cf = getClassFileFor(cu, typeStr);
			if (cf!=null) {
				Map<String, String> typeParamMap = createTypeParamMap(type, cf);
				addCompletionsForExtendedClass(retVal, cu, cf, pkg, typeParamMap);
			}
		}

	}


	/**
	 * Adds simple shorthand completions relevant to Java.
	 *
	 * @param set The set to add to.
	 */
	private void addShorthandCompletions(Set<Completion> set) {
		if (shorthandCache != null) {
			set.addAll(shorthandCache.getShorthandCompletions());
		}
	}

	/**
	 * Set template completion cache for source completion provider.
	 *
	 * @param shorthandCache The new cache.
	 */
	public void setShorthandCache(ShorthandCompletionCache shorthandCache) {
		this.shorthandCache = shorthandCache;
	}


	/**
	 * Gets the {@link ClassFile} for a class.
	 *
	 * @param cu The compilation unit being parsed.
	 * @param className The name of the class (fully qualified or not).
	 * @return The {@link ClassFile} for the class, or <code>null</code> if
	 *         <code>cf</code> represents <code>java.lang.Object</code> (or
	 *         if the super class could not be determined).
	 */
	private ClassFile getClassFileFor(CompilationUnit cu, String className) {

		//System.err.println(">>> Getting class file for: " + className);
		if (className==null) {
			return null;
		}

		ClassFile superClass = null;

		// Determine the fully qualified class to grab
		if (!Util.isFullyQualified(className)) {

			// Check in this source file's package first
			String pkg = cu.getPackageName();
			if (pkg!=null) {
				String temp = pkg + "." + className;
				superClass = jarManager.getClassEntry(temp);
			}

			// Next, go through the imports (order is important)
			if (superClass==null) {
				Iterator<ImportDeclaration> i = cu.getImportIterator();
				while (i.hasNext()) {
					ImportDeclaration id = i.next();
					String imported = id.getName();
					if (imported.endsWith(".*")) {
						String temp = imported.substring(
								0, imported.length()-1) + className;
						superClass = jarManager.getClassEntry(temp);
						if (superClass!=null) {
							break;
						}
					}
					else if (imported.endsWith("." + className)) {
						superClass = jarManager.getClassEntry(imported);
						break;
					}
				}
			}

			// Finally, try java.lang
			if (superClass==null) {
				String temp = "java.lang." + className;
				superClass = jarManager.getClassEntry(temp);
			}

		}

		else {
			superClass = jarManager.getClassEntry(className);
		}

		return superClass;

	}


	/**
	 * Adds completions for local variables in a method.
	 *
	 * @param set The set of completions to add to.
	 * @param method The  method being examined.
	 * @param offs The caret's offset into the source.  This should be inside
	 *        of <code>method</code>.
	 */
	private void addLocalVarCompletions(Set<Completion> set, Method method,
			int offs) {

		for (int i=0; i<method.getParameterCount(); i++) {
			FormalParameter param = method.getParameter(i);
			set.add(new LocalVariableCompletion(this, param));
		}

		CodeBlock body = method.getBody();
		if (body!=null) {
			addLocalVarCompletions(set, body, offs);
		}

	}


	/**
	 * Adds completions for local variables in a code block inside a method.
	 *
	 * @param set The set of completions to add to.
	 * @param block The code block.
	 * @param offs The caret's offset into the source. This should be inside
	 *        of <code>block</code>.
	 */
	private void addLocalVarCompletions(Set<Completion> set, CodeBlock block,
			int offs) {

		for (int i=0; i<block.getLocalVarCount(); i++) {
			LocalVariable var = block.getLocalVar(i);
			if (var.getNameEndOffset()<=offs) {
				set.add(new LocalVariableCompletion(this, var));
			}
			else { // This and all following declared after offs
				break;
			}
		}

		for (int i=0; i<block.getChildBlockCount(); i++) {
			CodeBlock child = block.getChildBlock(i);
			if (child.containsOffset(offs)) {
				addLocalVarCompletions(set, child, offs);
				break; // All other blocks are past this one
			}
			// If we've reached a block that's past the offset we're
			// searching for...
			else if (child.getNameStartOffset()>offs) {
				break;
			}
		}

	}


	/**
	 * Adds a jar to read from.
	 *
	 * @param info The jar to add.  If this is <code>null</code>, then
	 *        the current JVM's main JRE jar (rt.jar, or classes.jar on OS X)
	 *        will be added.  If this jar has already been added, adding it
	 *        again will do nothing (except possibly update its attached source
	 *        location).
	 * @throws IOException If an IO error occurs.
	 * @see #getJars()
	 * @see #removeJar(File)
	 */
	public void addJar(LibraryInfo info) throws IOException {
		jarManager.addClassFileSource(info);
	}


	/**
	 * Checks whether the user is typing a completion for a String member after
	 * a String literal.
	 *
	 * @param comp The text component.
	 * @param alreadyEntered The text already entered.
	 * @param cu The compilation unit being parsed.
	 * @param set The set to add possible completions to.
	 * @return Whether the user is indeed typing a completion for a String
	 *         literal member.
	 */
	private boolean checkStringLiteralMember(JTextComponent comp,
							String alreadyEntered,
							CompilationUnit cu, Set<Completion> set) {

		boolean stringLiteralMember = false;

		int offs = comp.getCaretPosition() - alreadyEntered.length() - 1;
		if (offs>1) {
			RSyntaxTextArea textArea = (RSyntaxTextArea)comp;
			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			try {
				//log(doc.charAt(offs) + ", " + doc.charAt(offs+1));
				if (doc.charAt(offs)=='"' && doc.charAt(offs+1)=='.') {
					int curLine = textArea.getLineOfOffset(offs);
					Token list = textArea.getTokenListForLine(curLine);
					Token prevToken = RSyntaxUtilities.getTokenAtOffset(list, offs);
					if (prevToken!=null &&
							prevToken.getType()==Token.LITERAL_STRING_DOUBLE_QUOTE) {
						ClassFile cf = getClassFileFor(cu, "java.lang.String");
						addCompletionsForExtendedClass(set, cu, cf,
													cu.getPackageName(), null);
						stringLiteralMember = true;
					}
				}
			} catch (BadLocationException ble) { // Never happens
				ble.printStackTrace();
			}
		}

		return stringLiteralMember;

	}


	/**
	 * Removes all jars from the "build path".
	 *
	 * @see #removeJar(File)
	 * @see #addJar(LibraryInfo)
	 * @see #getJars()
	 */
	public void clearJars() {
		jarManager.clearClassFileSources();
		// The memory used by the completions can be quite large, so go ahead
		// and clear out the completions list so no-longer-needed ones are
		// eligible for GC.
		clear();
	}


	/**
	 * Creates and returns a mapping of type parameters to type arguments.
	 *
	 * @param type The type of a variable/field/etc. whose fields/methods/etc.
	 *        are being code completed, as declared in the source.  This
	 *        includes type arguments.
	 * @param cf The <code>ClassFile</code> representing the actual type of
	 *        the variable/field/etc. being code completed
	 * @return A mapping of type parameter names to type arguments (both
	 *         Strings).
	 */
	private Map<String, String> createTypeParamMap(Type type, ClassFile cf) {
		Map<String, String> typeParamMap = null;
		List<TypeArgument> typeArgs = type.getTypeArguments(type.getIdentifierCount()-1);
		if (typeArgs!=null) {
			typeParamMap = new HashMap<>();
			List<String> paramTypes = cf.getParamTypes();
			// Should be the same size!  Otherwise, the source code has
			// too many/too few type arguments listed for this type.
			int min = Math.min(paramTypes==null ? 0 : paramTypes.size(),
									typeArgs.size());
			for (int i=0; i<min; i++) {
				TypeArgument typeArg = typeArgs.get(i);
				typeParamMap.put(paramTypes.get(i), typeArg.toString());
			}
		}
		return typeParamMap;
	}


	@Override
	public List<Completion> getCompletionsAt(JTextComponent tc, Point p) {
		getCompletionsImpl(tc); // Force loading of completions
		return super.getCompletionsAt(tc, p);
	}


	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {

		comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {

		completions = new ArrayList<>();//completions.clear();

		CompilationUnit cu = javaProvider.getCompilationUnit();
		if (cu==null) {
			return completions; // empty
		}

		Set<Completion> set = new TreeSet<>();

		// Cut down the list to just those matching what we've typed.
		// Note: getAlreadyEnteredText() never returns null
		String text = getAlreadyEnteredText(comp);

		// Special case - end of a String literal
		boolean stringLiteralMember = checkStringLiteralMember(comp, text, cu,
																set);

		// Not after a String literal - regular code completion
		if (!stringLiteralMember) {

			// Don't add shorthand completions if they're typing something
			// qualified
			if (text.indexOf('.')==-1) {
				addShorthandCompletions(set);
			}

			loadImportCompletions(set, text, cu);

			// Add completions for fully-qualified stuff (e.g. "com.sun.jav")
			//long startTime = System.currentTimeMillis();
			jarManager.addCompletions(this, text, set);
			//long time = System.currentTimeMillis() - startTime;
			//log("jar completions loaded in: " + time);

			// Loop through all types declared in this source, and provide
			// completions depending on in what type/method/etc. the caret's in.
			loadCompletionsForCaretPosition(cu, comp, text, set);

		}

		// Do a final sort of all of our completions and we're good to go!
		completions = new ArrayList<>(set);
		Collections.sort(completions);

		// Only match based on stuff after the final '.', since that's what is
		// displayed for all of our completions.
		text = text.substring(text.lastIndexOf('.')+1);

		@SuppressWarnings("unchecked")
		int start = Collections.binarySearch(completions, text, comparator);
		if (start<0) {
			start = -(start+1);
		}
		else {
			// There might be multiple entries with the same input text.
			while (start>0 &&
					comparator.compare(completions.get(start-1), text)==0) {
				start--;
			}
		}

		@SuppressWarnings("unchecked")
		int end = Collections.binarySearch(completions, text+'{', comparator);
		end = -(end+1);

		return completions.subList(start, end);

		} finally {
			comp.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		}

	}


	/**
	 * Returns the jars on the "build path".
	 *
	 * @return A list of {@link LibraryInfo}s.  Modifying a
	 *         <code>LibraryInfo</code> in this list will have no effect on
	 *         this completion provider; in order to do that, you must re-add
	 *         the jar via {@link #addJar(LibraryInfo)}. If there are
	 *         no jars on the "build path," this will be an empty list.
	 * @see #addJar(LibraryInfo)
	 */
	public List<LibraryInfo> getJars() {
		return jarManager.getClassFileSources();
	}

	JarManager getJarManager() {
		return jarManager;
	}

	void setJarManager(JarManager jarManager) {
		if (jarManager!=null) this.jarManager = jarManager;
	}

public SourceLocation getSourceLocForClass(String className) {
	return jarManager.getSourceLocForClass(className);
}

	/**
	 * Returns whether a method defined by a super class is accessible to
	 * this class.
	 *
	 * @param info Information about the member.
	 * @param pkg The package of the source currently being parsed.
	 * @return Whether the method is accessible.
	 */
	private boolean isAccessible(MemberInfo info, String pkg) {

		boolean accessible = false;
		int access = info.getAccessFlags();

		if (com.deckerpw.flateditor.lang.java.classreader.Util.isPublic(access) ||
				com.deckerpw.flateditor.lang.java.classreader.Util.isProtected(access)) {
			accessible = true;
		}
		else if (com.deckerpw.flateditor.lang.java.classreader.Util.isDefault(access)) {
			String pkg2 = info.getClassFile().getPackageName();
			accessible = (pkg==null && pkg2==null) ||
						(pkg!=null && pkg.equals(pkg2));
		}

		return accessible;

	}


	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {
		try {
			Document doc = comp.getDocument();
			int caret = comp.getCaretPosition();
			Element root = doc.getDefaultRootElement();
			int lineIndex = root.getElementIndex(caret);
			Element line = root.getElement(lineIndex);
			int start = line.getStartOffset();
			int len = caret - start;
			if (len <= 0) return "";
			doc.getText(start, len, seg);
			char[] array = seg.array;
			int segOff = seg.offset;
			int end = segOff + len;
			int p = end - 1;
			int depth = 0;
			while (p >= segOff) {
				char ch = array[p];
				if (ch == ')') {
					depth++;
					p--;
					continue;
				}
				if (ch == '(') {
					if (depth > 0) {
						depth--;
						p--;
						continue;
					}
					break;
				}
				if (depth > 0) {
					if (ch == ',' || Character.isWhitespace(ch)) {
						p--;
						continue;
					}
					p--;
					continue;
				}
				if (ch == '.') {
					p--;
					continue;
				}
				if (Character.isJavaIdentifierPart(ch) || ch == '$') {
					while (p >= segOff && (Character.isJavaIdentifierPart(array[p]) || array[p] == '$')) p--;
					continue;
				}
				if (Character.isWhitespace(ch)) break;
				break;
			}
			int exprStart = p + 1;
			int exprLen = end - exprStart;
			if (exprLen <= 0) return "";
			String raw = new String(array, exprStart, exprLen).trim();
			if (raw.endsWith(".")) {
				String withoutDot = raw.substring(0, raw.length() - 1);
				if (withoutDot.isEmpty() || withoutDot.endsWith(".")) return raw;
				return raw;
			}
			return raw;
		} catch (BadLocationException e) {
			return "";
		}
	}

	@Override
	protected boolean isValidChar(char ch) {
		return Character.isJavaIdentifierPart(ch) || ch=='.';
	}


	/**
	 * Loads completions based on the current caret location in the source.  In
	 * other words:
	 *
	 * <ul>
	 *   <li>If the caret is anywhere in a class, the names of all methods and
	 *       fields in the class are loaded.  Methods and fields in super
	 *       classes are also loaded.  TODO: Get super methods/fields added
	 *       correctly by access!
	 *   <li>If the caret is in a field, local variables currently accessible
	 *       are loaded.
	 * </ul>
	 *
	 * @param cu The compilation unit being parsed.
	 * @param comp The text component.
	 * @param alreadyEntered The already-entered text.
	 * @param retVal The set of values to add to.
	 */
	private void loadCompletionsForCaretPosition(CompilationUnit cu,
		JTextComponent comp, String alreadyEntered, Set<Completion> retVal) {

		// Get completions for all fields and methods of all type declarations.

		//long startTime = System.currentTimeMillis();
		int caret = comp.getCaretPosition();
		//List temp = new ArrayList();
		int start;
		int end;

		int lastDot = alreadyEntered.lastIndexOf('.');
		boolean qualified = lastDot>-1;
		String prefix = qualified ? alreadyEntered.substring(0, lastDot) : null;

		Iterator<TypeDeclaration> i = cu.getTypeDeclarationIterator();
		while (i.hasNext()) {

			TypeDeclaration td = i.next();
			start = td.getBodyStartOffset();
			end = td.getBodyEndOffset();

			if (caret>start && caret<=end) {
				loadCompletionsForCaretPosition(cu, comp, alreadyEntered,
									retVal, td, prefix, caret);
			}

			else if (caret<start) {
				break; // We've passed any type declarations we could be in
			}

		}

		//long time = System.currentTimeMillis() - startTime;
		//log("methods/fields/localvars loaded in: " + time);

	}


	/**
	 * Loads completions based on the current caret location in the source.
	 * This method is called when the caret is found to be in a specific type
	 * declaration.  This method checks if the caret is in a child type
	 * declaration first, then adds completions for itself next.
	 *
	 * <ul>
	 *   <li>If the caret is anywhere in a class, the names of all methods and
	 *       fields in the class are loaded.  Methods and fields in super
	 *       classes are also loaded.  TODO: Get super methods/fields added
	 *       correctly by access!
	 *   <li>If the caret is in a field, local variables currently accessible
	 *       are loaded.
	 * </ul>
	 *
	 * @param cu The compilation unit.
	 * @param comp The text component being analyzed.
	 * @param alreadyEntered The already-entered text.
	 * @param retVal The set of returned completions.
	 * @param td The type declaration.
	 * @param prefix The prefix.
	 * @param caret The caret position.
	 */
	private void loadCompletionsForCaretPosition(CompilationUnit cu,
			JTextComponent comp, String alreadyEntered, Set<Completion> retVal,
			TypeDeclaration td, String prefix, int caret) {

		// Do any child types first, so if any vars, etc. have duplicate names,
		// we pick up the one "closest" to us first.
		for (int i=0; i<td.getChildTypeCount(); i++) {
			TypeDeclaration childType = td.getChildType(i);
			loadCompletionsForCaretPosition(cu, comp, alreadyEntered, retVal,
					childType, prefix, caret);
		}

		Method currentMethod = null;

		Map<String, String> typeParamMap = new HashMap<>();
		if (td instanceof NormalClassDeclaration) {
			NormalClassDeclaration ncd = (NormalClassDeclaration)td;
			List<TypeParameter> typeParams = ncd.getTypeParameters();
			if (typeParams!=null) {
				for (TypeParameter typeParam : typeParams) {
					String typeVar = typeParam.getName();
					// For non-qualified completions, use type var name.
					typeParamMap.put(typeVar, typeVar);
				}
			}
		}

		// Get completions for this class's methods, fields and local
		// vars.  Do this before checking super classes so that, if
		// we overrode anything, we get the "newest" version.
		String pkg = cu.getPackageName();
		Iterator<Member> j = td.getMemberIterator();
		while (j.hasNext()) {
			Member m = j.next();
			if (m instanceof Method) {
				Method method = (Method)m;
				if (prefix==null || THIS.equals(prefix)) {
					retVal.add(new MethodCompletion(this, method));
				}
				if (caret>=method.getBodyStartOffset() && caret<method.getBodyEndOffset()) {
					currentMethod = method;
					// Don't add completions for local vars if there is
					// a prefix, even "this".
					if (prefix==null) {
						addLocalVarCompletions(retVal, method, caret);
					}
				}
			}
			else if (m instanceof Field) {
				if (prefix==null || THIS.equals(prefix)) {
					Field field = (Field)m;
					retVal.add(new FieldCompletion(this, field));
				}
			}
		}

		// Completions for superclass methods.
		// TODO: Implement me better
		if (prefix==null || THIS.equals(prefix)) {
			if (td instanceof NormalClassDeclaration) {
				NormalClassDeclaration ncd = (NormalClassDeclaration)td;
				Type extended = ncd.getExtendedType();
				if (extended!=null) { // e.g., not java.lang.Object
					String superClassName = extended.toString();
					ClassFile cf = getClassFileFor(cu, superClassName);
					if (cf!=null) {
						addCompletionsForExtendedClass(retVal, cu, cf, pkg, null);
					}
					else {
						log("[DEBUG]: Couldn't find ClassFile for: " + superClassName);
					}
				}
			}
		}

		if (prefix!=null) {
			boolean isThis = THIS.equals(prefix);
			boolean isThisChain = prefix.startsWith(THIS + ".");
			if (isThis) {
			}
			else if (isThisChain) {
				loadCompletionsForCaretPositionQualified(cu,
						alreadyEntered, retVal,
						td, currentMethod, prefix, caret);
			}
			else if (!isThis) {
				loadCompletionsForCaretPositionQualified(cu,
						alreadyEntered, retVal,
						td, currentMethod, prefix, caret);
			}
		}

	}


	/**
	 * Loads completions for the text at the current caret position, if there
	 * is a "prefix" of chars and at least one '.' character in the text up to
	 * the caret.  This is currently very limited and needs to be improved.
	 *
	 * @param cu The compilation unit being examined.
	 * @param alreadyEntered The already-entered text.
	 * @param retVal The return value.
	 * @param td The type declaration the caret is in.
	 * @param currentMethod The method the caret is in, or <code>null</code> if
	 *        none.
	 * @param prefix The text up to the current caret position.  This is
	 *        guaranteed to be non-<code>null</code> not equal to
	 *        "<tt>this</tt>".
	 * @param offs The offset of the caret in the document.
	 */
	private void loadCompletionsForCaretPositionQualified(CompilationUnit cu,
			String alreadyEntered, Set<Completion> retVal,
			TypeDeclaration td, Method currentMethod, String prefix, int offs) {
		String pkg = cu.getPackageName();
		ClassFile target = resolveChainToClassFile(cu, td, currentMethod, prefix, offs, pkg);
		if (target != null) {
			boolean isArray = isArrayTypeForChain(cu, td, currentMethod, prefix, offs);
			addCompletionsForExtendedClass(retVal, cu, target, pkg, null);
			if (isArray) {
				FieldCompletion fc = FieldCompletion.createLengthCompletion(this, new Type("Object", 1));
				retVal.add(fc);
			}
			return;
		}
		List<String> parts = splitChain(prefix);
		if (parts.size()==1) {
			String single = getBaseName(parts.get(0));
			if (!isMethodSegment(parts.get(0))) {
				for (Iterator<Member> j=td.getMemberIterator(); j.hasNext();) {
					Member m=j.next();
					if (m instanceof Field) {
						Field f=(Field)m;
						if (f.getName().equals(single)) {
							Type t=f.getType();
							if (t.isArray()) {
								ClassFile cf=getClassFileFor(cu,"java.lang.Object");
								addCompletionsForExtendedClass(retVal,cu,cf,pkg,null);
								retVal.add(FieldCompletion.createLengthCompletion(this,t));
							} else if(!t.isBasicType()){
								String ts=t.getName(true,false);
								ClassFile cf=getClassFileFor(cu,ts);
								if(cf!=null){
									Map<String,String> m2=createTypeParamMap(t,cf);
									addCompletionsForExtendedClass(retVal,cu,cf,pkg,m2);
								}
							}
							return;
						}
					}
				}
				if (currentMethod!=null) {
					for(int i=0;i<currentMethod.getParameterCount();i++){
						FormalParameter p=currentMethod.getParameter(i);
						if(p.getName().equals(single)){
							addCompletionsForLocalVarsMethods(cu,p,retVal);
							return;
						}
					}
					CodeBlock body=currentMethod.getBody();
					if(body!=null && findLocalVarType(body,single,offs)!=null){
						LocalVariable v=findLocalVar(body,single,offs);
						if(v!=null) addCompletionsForLocalVarsMethods(cu,v,retVal);
						return;
					}
				}
			}
			List<ImportDeclaration> imports=cu.getImports();
			List<ClassFile> matches=jarManager.getClassesWithUnqualifiedName(single, imports);
			if(matches!=null){
				for(ClassFile cf:matches) addCompletionsForStaticMembers(retVal,cu,cf,pkg);
			}
		} else {
			if (target==null) {
				ClassFile cf=getClassFileFor(cu,prefix);
				if(cf!=null) addCompletionsForStaticMembers(retVal,cu,cf,pkg);
				else {
					for(int k=parts.size();k>=1;k--){
						String cand=joinBaseNames(parts,0,k);
						boolean anyMethod=false;
						for(int t=0;t<k;t++) if(isMethodSegment(parts.get(t))){anyMethod=true;break;}
						if(anyMethod) continue;
						ClassFile c2=getClassFileFor(cu,cand);
						if(c2!=null){ addCompletionsForStaticMembers(retVal,cu,c2,pkg); break; }
					}
				}
			}
		}
	}

	private void loadCompletionsForCaretPositionQualifiedCodeBlock(
			CompilationUnit cu, Set<Completion> retVal,
			TypeDeclaration td, CodeBlock block, String prefix, int offs) {
		List<String> parts=splitChain(prefix);
		String first = parts.isEmpty()?prefix:getBaseName(parts.get(0));
		boolean found=false;
		for(int i=0;i<block.getLocalVarCount();i++){
			LocalVariable var=block.getLocalVar(i);
			if(var.getNameEndOffset()<=offs && var.getName().equals(first)){
				if(parts.size()==1){
					addCompletionsForLocalVarsMethods(cu,var,retVal);
				} else {
					ClassFile cf=resolveChainToClassFile(cu,td,null,prefix,offs,cu.getPackageName());
					if(cf!=null) addCompletionsForExtendedClass(retVal,cu,cf,cu.getPackageName(),null);
				}
				found=true; break;
			}
			if(var.getNameEndOffset()>offs) break;
		}
		if(found) return;
		for(int i=0;i<block.getChildBlockCount();i++){
			CodeBlock child=block.getChildBlock(i);
			if(child.containsOffset(offs)){
				loadCompletionsForCaretPositionQualifiedCodeBlock(cu,retVal,td,child,prefix,offs);
				break;
			} else if(child.getNameStartOffset()>offs) break;
		}
	}

	private ClassFile resolveChainToClassFile(CompilationUnit cu, TypeDeclaration td, Method currentMethod, String prefix, int offs, String pkg) {
		if(prefix==null||prefix.isEmpty()) return null;
		if(prefix.endsWith(".")) prefix=prefix.substring(0,prefix.length()-1);
		if(prefix.isEmpty()) return null;
		boolean startsWithThis=prefix.equals(THIS)||prefix.startsWith(THIS+".");
		String work=prefix;
		boolean isThisChain=false;
		if(startsWithThis){
			if(prefix.equals(THIS)) return null;
			work=prefix.substring(THIS.length()+1);
			isThisChain=true;
		}
		List<String> parts=splitChain(work);
		if(parts.isEmpty()) return null;
		for(String p:parts){ String b=getBaseName(p); if(b.isEmpty()||!b.matches("[A-Za-z_][A-Za-z0-9_\\$]*")) return null; }
		int maxDepth=20;
		if(parts.size()>maxDepth) return null;
		ClassFile current=null;
		String firstRaw=parts.get(0);
		String firstName=getBaseName(firstRaw);
		boolean firstIsMethod=isMethodSegment(firstRaw);
		if(!isThisChain){
			ClassFile staticCf=null;
			int staticEnd=-1;
			for(int k=parts.size();k>=1;k--){
				boolean anyMethod=false;
				for(int t=0;t<k;t++) if(isMethodSegment(parts.get(t))){anyMethod=true;break;}
				if(anyMethod) continue;
				String cand=joinBaseNames(parts,0,k);
				ClassFile candCf=getClassFileFor(cu,cand);
				if(candCf!=null){ staticCf=candCf; staticEnd=k-1; break; }
			}
			if(staticCf!=null){
				if(staticEnd==parts.size()-1) return staticCf;
				current=staticCf;
				for(int i=staticEnd+1;i<parts.size();i++){
					current=resolveNext(current, parts.get(i), cu, pkg);
					if(current==null) return null;
					if(isVoidType(current)) return null;
				}
				return current;
			}
		}
		if(isThisChain){
			TypeAndFile taf=resolveMemberOfThis(parts.get(0), cu, td, pkg);
			if(taf==null) return null;
			current=taf.cf;
			for(int i=1;i<parts.size();i++){
				current=resolveNext(current, parts.get(i), cu, pkg);
				if(current==null) return null;
			}
			return current;
		}
		if(firstIsMethod){
			TypeAndFile taf=resolveMemberOfThis(firstRaw, cu, td, pkg);
			if(taf==null) return null;
			current=taf.cf;
		} else {
			Type varType=findTypeForIdentifier(firstName, td, currentMethod, offs, cu);
			if(varType!=null){
				if(varType.isBasicType()) return null;
				if(varType.isArray()){
					String elem="java.lang.Object";
					current=getClassFileFor(cu,elem);
				} else {
					String ts=varType.getName(true,false);
					current=getClassFileFor(cu,ts);
					if(current!=null){
						Map<String,String> mp=createTypeParamMap(varType,current);
						current.setTypeParamsToTypeArgs(mp);
					}
				}
			}
			if(current==null){
				TypeAndFile taf=resolveMemberOfThis(firstRaw, cu, td, pkg);
				if(taf!=null) current=taf.cf;
			}
			if(current==null){
				List<ClassFile> m=jarManager.getClassesWithUnqualifiedName(firstName, cu.getImports());
				if(m!=null&&!m.isEmpty()) current=m.get(0);
			}
			if(current==null) return null;
		}
		for(int i=1;i<parts.size();i++){
			current=resolveNext(current, parts.get(i), cu, pkg);
			if(current==null) return null;
		}
		return current;
	}

	private static class TypeAndFile { ClassFile cf; Type t; TypeAndFile(ClassFile c, Type tt){cf=c;t=tt;} }

	private TypeAndFile resolveMemberOfThis(String rawSeg, CompilationUnit cu, TypeDeclaration td, String pkg){
		String name=getBaseName(rawSeg);
		boolean isMethod=isMethodSegment(rawSeg);
		int argCount=isMethod?getArgCount(rawSeg):-1;
		for(Iterator<Member> it=td.getMemberIterator(); it.hasNext();){
			Member m=it.next();
			if(m instanceof Field){
				Field f=(Field)m;
				if(f.getName().equals(name) && !isMethod){
					Type t=f.getType();
					if(t.isBasicType()||t.isArray()) {
						if(t.isArray()){
							ClassFile cf=getClassFileFor(cu,"java.lang.Object");
							return new TypeAndFile(cf,t);
						}
						return null;
					}
					String ts=t.getName(true,false);
					ClassFile cf=getClassFileFor(cu,ts);
					if(cf!=null) return new TypeAndFile(cf,t);
				}
			}
		}
		List<Method> methods=new ArrayList<>();
		for(Iterator<Member> it=td.getMemberIterator(); it.hasNext();){
			Member m=it.next();
			if(m instanceof Method){
				Method mm=(Method)m;
				if(mm.getName().equals(name)) methods.add(mm);
			}
		}
		if(!methods.isEmpty()){
			Method chosen=methods.get(0);
			Type ret=chosen.getType();
			if(ret==null) return null;
			if(ret.isBasicType()) return null;
			if(ret.toString().equals("void")) return null;
			if(ret.isArray()){
				ClassFile cf=getClassFileFor(cu,"java.lang.Object");
				return new TypeAndFile(cf,ret);
			}
			String ts=ret.getName(true,false);
			ClassFile cf=getClassFileFor(cu,ts);
			if(cf!=null) return new TypeAndFile(cf,ret);
		}
		String superName=null;
		if(td instanceof NormalClassDeclaration){
			Type ext=((NormalClassDeclaration)td).getExtendedType();
			if(ext!=null) superName=ext.toString();
		}
		ClassFile cf=null;
		if(superName!=null) cf=getClassFileFor(cu,superName);
		while(cf!=null){
			FieldInfo fi=findFieldInHierarchy(cf,name,cu,pkg);
			if(fi!=null && !isMethod){
				String ts=fi.getTypeString(true);
				boolean arr=ts.endsWith("[]");
				String base=arr?ts.substring(0,ts.length()-2):ts;
				if(isPrimitive(base)) return null;
				ClassFile ncf=getClassFileFor(cu,base);
				if(ncf!=null) return new TypeAndFile(ncf,null);
			}
			List<MethodInfo> mis=findMethodsInHierarchy(cf,name,cu,pkg);
			if(mis!=null&&!mis.isEmpty()){
				MethodInfo chosen=chooseMethod(mis,argCount);
				if(chosen!=null){
					String ret=chosen.getReturnTypeString(true);
					if("void".equals(ret)) return null;
					boolean arr=ret.endsWith("[]");
					String base=arr? ret.substring(0,ret.length()-2):ret;
					base=base.split("<")[0];
					if(isPrimitive(base)) return null;
					String mapped=cf.getTypeArgument(base);
					List<String> pts=cf.getParamTypes();
					if(mapped!=null&&pts!=null&&pts.contains(base)) base=mapped.split("<")[0];
					ClassFile ncf=getClassFileFor(cu,base);
					if(ncf!=null) return new TypeAndFile(ncf,null);
				}
			}
			String sup=cf.getSuperClassName(true);
			cf=sup==null?null:getClassFileFor(cu,sup);
		}
		return null;
	}

	private ClassFile resolveNext(ClassFile cur, String rawSeg, CompilationUnit cu, String pkg){
		String name=getBaseName(rawSeg);
		boolean isMethod=isMethodSegment(rawSeg);
		int argCount=isMethod?getArgCount(rawSeg):-1;
		if("length".equals(name) && !isMethod){
			return null;
		}
		FieldInfo fi=null;
		if(!isMethod) fi=findFieldInHierarchy(cur,name,cu,pkg);
		if(fi!=null){
			String ts=fi.getTypeString(true);
			boolean arr=ts.endsWith("[]");
			String base=arr?ts.substring(0,ts.length()-2):ts;
			if(isPrimitive(base)) return null;
			base=base.split("<")[0];
			String mapped=cur.getTypeArgument(base);
			List<String> pts=cur.getParamTypes();
			if(mapped!=null&&pts!=null&&pts.contains(base)) base=mapped.split("<")[0];
			return getClassFileFor(cu,base);
		}
		List<MethodInfo> mis=findMethodsInHierarchy(cur,name,cu,pkg);
		if(mis==null||mis.isEmpty()) return null;
		MethodInfo chosen=chooseMethod(mis,argCount);
		if(chosen==null) return null;
		String ret=chosen.getReturnTypeString(true);
		if("void".equals(ret)) return null;
		boolean arr=ret.endsWith("[]");
		String base=arr?ret.substring(0,ret.length()-2):ret;
		base=base.split("<")[0];
		if(isPrimitive(base)) return null;
		String mapped=cur.getTypeArgument(base);
		List<String> pts=cur.getParamTypes();
		if(mapped!=null&&pts!=null&&pts.contains(base)) base=mapped.split("<")[0];
		base=base.split("<")[0];
		return getClassFileFor(cu,base);
	}

	private Type findTypeForIdentifier(String name, TypeDeclaration td, Method m, int offs, CompilationUnit cu){
		if(m!=null){
			for(int i=0;i<m.getParameterCount();i++){
				FormalParameter p=m.getParameter(i);
				if(p.getName().equals(name)) return p.getType();
			}
			CodeBlock body=m.getBody();
			if(body!=null){
				LocalVariable v=findLocalVar(body,name,offs);
				if(v!=null) return v.getType();
			}
		}
		for(Iterator<Member> it=td.getMemberIterator(); it.hasNext();){
			Member mem=it.next();
			if(mem instanceof Field){
				Field f=(Field)mem;
				if(f.getName().equals(name)) return f.getType();
			}
		}
		return null;
	}

	private LocalVariable findLocalVar(CodeBlock block, String name, int offs){
		for(int i=0;i<block.getLocalVarCount();i++){
			LocalVariable v=block.getLocalVar(i);
			if(v.getName().equals(name) && v.getNameEndOffset()<=offs) return v;
		}
		for(int i=0;i<block.getChildBlockCount();i++){
			CodeBlock c=block.getChildBlock(i);
			if(c.containsOffset(offs)){
				LocalVariable v=findLocalVar(c,name,offs);
				if(v!=null) return v;
			} else if(c.getNameStartOffset()>offs) break;
		}
		return null;
	}

	private Type findLocalVarType(CodeBlock block, String name, int offs){
		LocalVariable v=findLocalVar(block,name,offs);
		return v==null?null:v.getType();
	}

	private List<String> splitChain(String s){
		List<String> out=new ArrayList<>();
		StringBuilder cur=new StringBuilder();
		int depth=0;
		for(int i=0;i<s.length();i++){
			char c=s.charAt(i);
			if(c=='(') depth++;
			else if(c==')'){ if(depth>0) depth--; }
			if(c=='.' && depth==0){ out.add(cur.toString().trim()); cur.setLength(0); }
			else cur.append(c);
		}
		if(cur.length()>0) out.add(cur.toString().trim());
		out.removeIf(String::isEmpty);
		return out;
	}

	private String getBaseName(String seg){
		int paren=seg.indexOf('(');
		if(paren>-1) return seg.substring(0,paren).trim();
		return seg.trim();
	}

	private String getLastSegmentBaseName(String prefix){
		List<String> p=splitChain(prefix);
		if(p.isEmpty()) return null;
		return getBaseName(p.get(p.size()-1));
	}

	private String joinBaseNames(List<String> parts, int from, int to){
		StringBuilder sb=new StringBuilder();
		for(int i=from;i<to;i++){
			if(i>from) sb.append('.');
			sb.append(getBaseName(parts.get(i)));
		}
		return sb.toString();
	}

	private boolean isMethodSegment(String seg){ return seg.indexOf('(')!=-1; }

	private int getArgCount(String seg){
		int l=seg.indexOf('(');
		int r=seg.lastIndexOf(')');
		if(l==-1||r==-1||r<l) return -1;
		String inside=seg.substring(l+1,r).trim();
		if(inside.isEmpty()) return 0;
		int cnt=1; int depth=0;
		for(int i=0;i<inside.length();i++){
			char c=inside.charAt(i);
			if(c=='(') depth++;
			else if(c==')') depth--;
			else if(c==',' && depth==0) cnt++;
		}
		return cnt;
	}

	private boolean isPrimitive(String t){
		return "byte".equals(t)||"short".equals(t)||"int".equals(t)||"long".equals(t)||"float".equals(t)||"double".equals(t)||"char".equals(t)||"boolean".equals(t)||"void".equals(t);
	}

	private boolean isArrayTypeForChain(CompilationUnit cu, TypeDeclaration td, Method m, String prefix, int offs){
		List<String> parts=splitChain(prefix);
		if(parts.isEmpty()) return false;
		List<String> withoutLast=parts.subList(0,parts.size()-1);
		if(withoutLast.isEmpty()){
			String n=getBaseName(parts.get(0));
			Type t=findTypeForIdentifier(n,td,m,offs,cu);
			return t!=null&&t.isArray();
		}
		String prefWithoutLast=String.join(".",withoutLast);
		ClassFile cf=resolveChainToClassFile(cu,td,m,prefWithoutLast,offs,cu.getPackageName());
		if(cf==null) return false;
		String last=getBaseName(parts.get(parts.size()-1));
		FieldInfo fi=findFieldInHierarchy(cf,last,cu,cu.getPackageName());
		if(fi!=null) return fi.getTypeString(true).endsWith("[]");
		List<MethodInfo> mis=findMethodsInHierarchy(cf,last,cu,cu.getPackageName());
		if(mis!=null&&!mis.isEmpty()){
			MethodInfo ch=chooseMethod(mis,-1);
			if(ch!=null) return ch.getReturnTypeString(true).endsWith("[]");
		}
		return false;
	}

	private boolean isVoidType(ClassFile cf){ return false; }

	private FieldInfo findFieldInHierarchy(ClassFile cf, String name, CompilationUnit cu, String pkg){
		ClassFile cur=cf;
		while(cur!=null){
			for(int i=0;i<cur.getFieldCount();i++){
				FieldInfo fi=cur.getFieldInfo(i);
				if(fi.getName().equals(name) && isAccessible(fi,pkg)) return fi;
			}
			String sup=cur.getSuperClassName(true);
			cur=sup==null?null:getClassFileFor(cu,sup);
		}
		if(cf!=null){
			for(int i=0;i<cf.getImplementedInterfaceCount();i++){
				String inter=cf.getImplementedInterfaceName(i,true);
				ClassFile icf=getClassFileFor(cu,inter);
				FieldInfo f=findFieldInHierarchy(icf,name,cu,pkg);
				if(f!=null) return f;
			}
		}
		return null;
	}

	private List<MethodInfo> findMethodsInHierarchy(ClassFile cf, String name, CompilationUnit cu, String pkg){
		List<MethodInfo> out=new ArrayList<>();
		ClassFile cur=cf;
		while(cur!=null){
			List<MethodInfo> mis=cur.getMethodInfoByName(name);
			if(mis!=null){
				for(MethodInfo mi:mis) if(isAccessible(mi,pkg) && !mi.isConstructor()) out.add(mi);
			}
			String sup=cur.getSuperClassName(true);
			cur=sup==null?null:getClassFileFor(cu,sup);
		}
		if(out.isEmpty() && cf!=null){
			for(int i=0;i<cf.getImplementedInterfaceCount();i++){
				String inter=cf.getImplementedInterfaceName(i,true);
				ClassFile icf=getClassFileFor(cu,inter);
				if(icf!=null){
					List<MethodInfo> mis=icf.getMethodInfoByName(name);
					if(mis!=null) for(MethodInfo mi:mis) if(isAccessible(mi,pkg)) out.add(mi);
				}
			}
		}
		return out;
	}

	private MethodInfo chooseMethod(List<MethodInfo> list, int argCount){
		if(list==null||list.isEmpty()) return null;
		if(argCount<0) return list.get(0);
		for(MethodInfo mi:list) if(mi.getParameterCount()==argCount) return mi;
		return list.get(0);
	}


	/**
	 * Loads completions for a single import statement.
	 *
	 * @param importStr The import statement.
	 * @param pkgName The package of the source currently being parsed.
	 */
	private void loadCompletionsForImport(Set<Completion> set,
			String importStr, String pkgName) {

		if (importStr.endsWith(".*")) {
			String pkg = importStr.substring(0, importStr.length()-2);
			boolean inPkg = pkg.equals(pkgName);
			List<ClassFile> classes= jarManager.getClassesInPackage(pkg, inPkg);
			for (ClassFile cf : classes) {
				set.add(new ClassCompletion(this, cf));
			}
		}

		else {
			ClassFile cf = jarManager.getClassEntry(importStr);
			if (cf!=null) {
				set.add(new ClassCompletion(this, cf));
			}
		}

	}


	/**
	 * Loads completions for all import statements.
	 *
	 * @param cu The compilation unit being parsed.
	 */
	private void loadImportCompletions(Set<Completion> set, String text,
									CompilationUnit cu) {

		// Fully-qualified completions are handled elsewhere, so no need to
		// duplicate the work here
		if (text.indexOf('.')>-1) {
			return;
		}

		//long startTime = System.currentTimeMillis();

		String pkgName = cu.getPackageName();
		loadCompletionsForImport(set, JAVA_LANG_PACKAGE, pkgName);
		for (Iterator<ImportDeclaration> i=cu.getImportIterator(); i.hasNext();) {
			ImportDeclaration id = i.next();
			String name = id.getName();
			if (!JAVA_LANG_PACKAGE.equals(name)) {
				loadCompletionsForImport(set, name, pkgName);
			}
		}
		//Collections.sort(completions);

		//long time = System.currentTimeMillis() - startTime;
		//log("imports loaded in: " + time);

	}


	private static void log(String text) {
		LOG.log(System.Logger.Level.INFO, text);
	}


	/**
	 * Removes a jar from the "build path".
	 *
	 * @param jar The jar to remove.
	 * @return Whether the jar was removed.  This will be <code>false</code>
	 *         if the jar was not on the build path.
	 * @see #addJar(LibraryInfo)
	 * @see #getJars()
	 * @see #clearJars()
	 */
	public boolean removeJar(File jar) {
		boolean removed = jarManager.removeClassFileSource(jar);
		// The memory used by the completions can be quite large, so go ahead
		// and clear out the completions list so no-longer-needed ones are
		// eligible for GC.
		if (removed) {
			clear();
		}
		return removed;
	}


	/**
	 * Sets the parent Java provider.
	 *
	 * @param javaProvider The parent completion provider.
	 */
	void setJavaProvider(JavaCompletionProvider javaProvider) {
		this.javaProvider = javaProvider;
	}


}
