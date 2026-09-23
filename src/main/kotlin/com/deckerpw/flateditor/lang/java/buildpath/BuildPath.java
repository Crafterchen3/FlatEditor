package com.deckerpw.flateditor.lang.java.buildpath;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BuildPath {
	private final LibraryInfo jdk;
	private final List<LibraryInfo> projectLibs;

	public BuildPath(LibraryInfo jdk, List<LibraryInfo> projectLibs) {
		this.jdk = jdk;
		this.projectLibs = projectLibs==null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(projectLibs));
	}

	public LibraryInfo getJdk() { return jdk; }
	public List<LibraryInfo> getProjectLibs() { return projectLibs; }

	public List<LibraryInfo> getAllLibraries() {
		List<LibraryInfo> all = new ArrayList<>();
		if (jdk!=null) all.add(jdk);
		all.addAll(projectLibs);
		return Collections.unmodifiableList(all);
	}

	public static BuildPath of(Path jdkHome, List<Path> projectJars) {
		LibraryInfo jdk = null;
		if (jdkHome!=null) jdk = LibraryInfo.getJreJarInfo(jdkHome.toFile());
		List<LibraryInfo> libs = new ArrayList<>();
		if (projectJars!=null) for (Path p : projectJars) libs.add(LibraryInfo.fromFile(p.toFile()));
		return new BuildPath(jdk, libs);
	}

	public static BuildPath ofFiles(File jdkHome, List<File> projectJars) {
		LibraryInfo jdk = null;
		if (jdkHome!=null) jdk = LibraryInfo.getJreJarInfo(jdkHome);
		List<LibraryInfo> libs = new ArrayList<>();
		if (projectJars!=null) for (File f : projectJars) libs.add(LibraryInfo.fromFile(f));
		return new BuildPath(jdk, libs);
	}

	public static BuildPath ofInfos(LibraryInfo jdk, List<LibraryInfo> libs) {
		return new BuildPath(jdk, libs);
	}

	@Override public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof BuildPath)) return false;
		BuildPath other=(BuildPath)o;
		return Objects.equals(jdk, other.jdk) && Objects.equals(projectLibs, other.projectLibs);
	}
	@Override public int hashCode() { return Objects.hash(jdk, projectLibs); }
	@Override public String toString() { return "BuildPath{jdk="+jdk+", libs="+projectLibs+"}"; }
}
