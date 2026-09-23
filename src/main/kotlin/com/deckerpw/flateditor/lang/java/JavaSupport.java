package com.deckerpw.flateditor.lang.java;

import com.deckerpw.flateditor.lang.java.buildpath.BuildPath;
import com.deckerpw.flateditor.lang.java.buildpath.LibraryInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public final class JavaSupport {

	private static final JavaSupport INSTANCE = new JavaSupport();
	private final Map<RSyntaxTextArea, JavaLanguageSupport> areaToSupport = new HashMap<>();
	private final Map<RSyntaxTextArea, BuildPath> areaToBuildPath = new HashMap<>();

	private JavaSupport() {}

	public static JavaSupport get() { return INSTANCE; }

	public void register(RSyntaxTextArea textArea) {
		register(textArea, (BuildPath)null);
	}

	public void register(RSyntaxTextArea textArea, Path jdkHome, List<Path> projectLibs) {
		register(textArea, BuildPath.of(jdkHome, projectLibs));
	}

	public void register(RSyntaxTextArea textArea, File jdkHome, List<File> projectLibs) {
		register(textArea, BuildPath.ofFiles(jdkHome, projectLibs));
	}

	public void register(RSyntaxTextArea textArea, BuildPath buildPath) {
		unregister(textArea);
		JavaLanguageSupport support = new JavaLanguageSupport();
		if (buildPath!=null) {
			try { support.setBuildPath(buildPath); } catch (Exception e) { e.printStackTrace(); }
			areaToBuildPath.put(textArea, buildPath);
		}
		support.install(textArea, buildPath);
		areaToSupport.put(textArea, support);
	}

	public void unregister(RSyntaxTextArea textArea) {
		JavaLanguageSupport support = areaToSupport.remove(textArea);
		if (support!=null) support.uninstall(textArea);
		areaToBuildPath.remove(textArea);
	}

	public JavaLanguageSupport getSupport(RSyntaxTextArea textArea) {
		return areaToSupport.get(textArea);
	}

	public BuildPath getBuildPath(RSyntaxTextArea textArea) {
		return areaToBuildPath.get(textArea);
	}

	public void setBuildPath(RSyntaxTextArea textArea, BuildPath buildPath) {
		JavaLanguageSupport support = areaToSupport.get(textArea);
		if (support!=null) {
			support.setBuildPath(textArea, buildPath);
			if (buildPath!=null) areaToBuildPath.put(textArea, buildPath);
			else areaToBuildPath.remove(textArea);
		}
	}

	public void setJdk(RSyntaxTextArea textArea, Path jdkHome) {
		setJdk(textArea, jdkHome==null?null:jdkHome.toFile());
	}

	public void setJdk(RSyntaxTextArea textArea, File jdkHome) {
		JavaLanguageSupport support = areaToSupport.get(textArea);
		if (support!=null) {
			support.setJdk(textArea, jdkHome);
			BuildPath bp = support.getBuildPath(textArea);
			if (bp!=null) areaToBuildPath.put(textArea, bp);
		}
	}

	public void setProjectLibs(RSyntaxTextArea textArea, List<Path> libs) {
		List<LibraryInfo> infos = new ArrayList<>();
		if (libs!=null) for (Path p : libs) infos.add(LibraryInfo.fromFile(p.toFile()));
		setProjectLibInfos(textArea, infos);
	}

	public void setProjectLibInfos(RSyntaxTextArea textArea, List<LibraryInfo> libs) {
		JavaLanguageSupport support = areaToSupport.get(textArea);
		if (support!=null) {
			support.setProjectLibs(textArea, libs);
			BuildPath bp = support.getBuildPath(textArea);
			if (bp!=null) areaToBuildPath.put(textArea, bp);
		}
	}

	public void addJar(RSyntaxTextArea textArea, Path jar) {
		addJar(textArea, jar.toFile());
	}

	public void addJar(RSyntaxTextArea textArea, File jar) {
		JavaLanguageSupport support = areaToSupport.get(textArea);
		if (support!=null) support.addJar(textArea, jar);
		BuildPath bp = support==null?null:support.getBuildPath(textArea);
		if (bp!=null) areaToBuildPath.put(textArea, bp);
	}

	public JavaParser getParser(RSyntaxTextArea textArea) {
		JavaLanguageSupport support = areaToSupport.get(textArea);
		return support==null? null : support.getParser(textArea);
	}

	public JavaCompletionProvider getCompletionProvider(RSyntaxTextArea textArea) {
		JavaLanguageSupport support = areaToSupport.get(textArea);
		return support==null? null : support.getCompletionProvider(textArea);
	}

	public java.util.Collection<JavaLanguageSupport> getAllSupports() {
		return java.util.Collections.unmodifiableCollection(areaToSupport.values());
	}
}
