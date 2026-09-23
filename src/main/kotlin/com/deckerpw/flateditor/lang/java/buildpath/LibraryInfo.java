/*
 * 04/21/2012
 *
 * Copyright (C) 2010 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSTALanguageSupport.License.txt file for details.
 */
package com.deckerpw.flateditor.lang.java.buildpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dialog;

import com.deckerpw.flateditor.lang.java.JarManager;
import com.deckerpw.flateditor.lang.java.PackageMapNode;
import com.deckerpw.flateditor.lang.java.classreader.ClassFile;


/**
 * Information about a jar, compiled class folder, or other source of classes
 * to add to the "build path" for Java completion.  Instances of this class are
 * added to a {@link JarManager} for each library that should be on the build
 * path.<p>
 *
 * This class also keeps track of an optional source location, such as a zip
 * file or source folder.  If defined, this location is used to find the .java
 * source corresponding to the library's classes, which is used to display
 * Javadoc comments during code completion.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see DirLibraryInfo
 * @see JarLibraryInfo
 * @see ClasspathLibraryInfo
 */
public abstract class LibraryInfo implements Comparable<LibraryInfo>,
		Cloneable {

	private static final Logger LOG = System.
		getLogger(LibraryInfo.class.getName());

	/**
	 * The location of the source files corresponding to this library.  This
	 * may be <code>null</code>.
	 */
	private SourceLocation sourceLoc;


	/**
	 * Does any cleanup necessary after a call to
	 * {@link #bulkClassFileCreationStart()}.
	 *
	 * @throws IOException If an IO error occurs.
	 * @see #bulkClassFileCreationStart()
	 * @see #createClassFileBulk(String)
	 */
	public abstract void bulkClassFileCreationEnd() throws IOException;


	/**
	 * Readies this library for many class files being fetched via
	 * {@link #createClassFileBulk(String)}.  After calling this method,
	 * the actual class file fetching should be done in a try/finally block
	 * that ensures a call to {@link #bulkClassFileCreationEnd()}; e.g.
	 *
	 * <pre>
	 * libInfo.bulkClassFileCreationStart();
	 * try {
	 *    String entryName = ...;
	 *    ClassFile cf = createClassFileBulk(entryName);
	 *    ...
	 * } finally {
	 *    libInfo.bulkClassFileCreationEnd();
	 * }
	 * </pre>
	 *
	 * @throws IOException If an IO error occurs.
	 * @see #bulkClassFileCreationEnd()
	 * @see #createClassFileBulk(String)
	 */
	public abstract void bulkClassFileCreationStart() throws IOException;


	/**
	 * Returns a deep copy of this library.
	 *
	 * @return A deep copy.
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException cnse) { // Never happens
			throw new IllegalStateException(
			"Doesn't support cloning, but should! - " + getClass().getName());
		}
	}


	/**
	 * Returns the class file information for the specified class.  Instances
	 * of <code>JarReader</code> can call this method to lazily load
	 * information on individual classes and shove it into their package maps.
	 * <p>
	 * If many class files will be fetched at a time, you should prefer using
	 * {@link #bulkClassFileCreationStart()} and
	 * {@link #createClassFileBulk(String)} over this method, for performance
	 * reasons.
	 *
	 * @param entryName The fully qualified name of the class file.
	 * @return The class file, or <code>null</code> if it isn't found in this
	 *         library.
	 * @throws IOException If an IO error occurs.
	 * @see #createClassFileBulk(String)
	 */
	public abstract ClassFile createClassFile(String entryName) throws IOException;


	/**
	 * Returns the class file information for the specified class.  Instances
	 * of <code>JarReader</code> can call this method to lazily load
	 * information on individual classes and shove it into their package maps.
	 * <p>
	 * This method should be used when multiple classes will be fetched from
	 * this library at the same time.  It should only be called after a call to
	 * {@link #bulkClassFileCreationStart()}.  If only a single class file is
	 * being fetched, it is simpler to call {@link #createClassFile(String)}.
	 *
	 * @param entryName The fully qualified name of the class file.
	 * @return The class file, or <code>null</code> if it isn't found in this
	 *         library.
	 * @throws IOException If an IO error occurs.
	 * @see #createClassFile(String)
	 */
	public abstract ClassFile createClassFileBulk(String entryName)
			throws IOException;


	/**
	 * Creates and returns a map of maps representing the hierarchical package
	 * structure in this library.
	 *
	 * @return The package structure in this library.
	 * @throws IOException If an IO error occurs.
	 */
	public abstract PackageMapNode createPackageMap() throws IOException;


	/**
	 * Two <code>LibraryInfo</code>s are considered equal if they represent
	 * the same class file location.  Source attachment is irrelevant.
	 *
	 * @return Whether the specified instance represents the same class
	 *         source as this one.
	 */
	@Override
	public boolean equals(Object o) {
		return o instanceof LibraryInfo &&
				compareTo((LibraryInfo)o)==0;
	}


	/**
	 * Returns information on the "main" jar for a JRE.  This will be
	 * <tt>rt.jar</tt> everywhere except OS X, where it will be
	 * <tt>classes.jar</tt>.  The associated source zip/jar file is also
	 * checked for.
	 *
	 * @param jreHome The location of the JRE.
	 * @return The information, or <code>null</code> if there is not a JRE in
	 *         the specified directory.
	 * @see #getMainJreJarInfo()
	 */
	public static LibraryInfo getJreJarInfo(File jreHome) {
		if (jreHome==null) return null;
		File mods = new File(jreHome,"jmods");
		if (mods.isDirectory()) {
			File[] files = mods.listFiles(pathname -> {
				if (pathname.isFile()) {
					String name = pathname.getName();
					return name.endsWith(".jmod") &&
						(name.startsWith("java.") || name.startsWith("jdk."));
				}
				return false;
			});
			if (files!=null && files.length>0) {
				LibraryInfo info = new Jdk9LibraryInfo(files);
				for (File cand : new File[]{
						new File(jreHome,"lib/src.zip"),
						new File(jreHome,"src.zip"),
						new File(jreHome,"../src.zip")}) {
					if (cand.isFile()) { info.setSourceLocation(new ZipSourceLocation(cand)); break; }
				}
				return info;
			}
		}
		LibraryInfo info = null;
		File mainJar = new File(jreHome, "lib/rt.jar");
		File sourceZip;
		if (mainJar.isFile()) {
			sourceZip = new File(jreHome, "src.zip");
			if (!sourceZip.isFile()) sourceZip = new File(jreHome, "../src.zip");
			if (!sourceZip.isFile()) sourceZip = new File(jreHome, "lib/src.zip");
		}
		else {
			mainJar = new File(jreHome, "../Classes/classes.jar");
			sourceZip = new File(jreHome, "src.jar");
			if (!sourceZip.isFile()) sourceZip = new File(jreHome, "lib/src.zip");
		}
		if (mainJar.isFile()) {
			info = new JarLibraryInfo(mainJar);
			if (sourceZip.isFile()) info.setSourceLocation(new ZipSourceLocation(sourceZip));
		}
		else {
			LOG.log(System.Logger.Level.ERROR, "Cannot locate JRE jar in " + jreHome.getAbsolutePath());
		}
		return info;
	}

	public static LibraryInfo fromFile(File file) {
		return fromFile(file, null);
	}

	public static LibraryInfo fromFile(File file, File source) {
		if (file==null) throw new IllegalArgumentException("file cannot be null");
		SourceLocation srcLoc = null;
		if (source!=null) {
			if (source.isDirectory()) srcLoc = new DirSourceLocation(source);
			else if (source.isFile()) srcLoc = new ZipSourceLocation(source);
		}
		if (file.isDirectory()) {
			File mods = new File(file, "jmods");
			if (mods.isDirectory()) {
				LibraryInfo jdk = getJreJarInfo(file);
				if (jdk!=null) { if (srcLoc!=null) jdk.setSourceLocation(srcLoc); return jdk; }
			}
			File rt = new File(file, "lib/rt.jar");
			if (rt.isFile()) {
				LibraryInfo jdk = getJreJarInfo(file);
				if (jdk!=null) { if (srcLoc!=null) jdk.setSourceLocation(srcLoc); return jdk; }
			}
			DirLibraryInfo dirInfo = new DirLibraryInfo(file);
			if (srcLoc!=null) dirInfo.setSourceLocation(srcLoc);
			return dirInfo;
		} else if (file.isFile()) {
			String name = file.getName().toLowerCase();
			if (name.endsWith(".jar") || name.endsWith(".zip")) {
				JarLibraryInfo jarInfo = new JarLibraryInfo(file);
				if (srcLoc!=null) jarInfo.setSourceLocation(srcLoc);
				return jarInfo;
			}
		}
		throw new IllegalArgumentException("Unsupported library file: " + file.getAbsolutePath());
	}

	public static LibraryInfo fromPath(java.nio.file.Path path) {
		return fromFile(path.toFile());
	}

	public static LibraryInfo fromPath(java.nio.file.Path binary, java.nio.file.Path source) {
		return fromFile(binary.toFile(), source==null?null:source.toFile());
	}


	/**
	 * Returns the time this library was last modified.  For jar files, this
	 * would be the modified date of the file.  For directories, this would be
	 * the time a file in the directory was most recently modified.  This
	 * information is used to determine whether callers should clear their
	 * cached package map information and load it anew.<p>
	 *
	 * This API may change in the future.
	 *
	 * @return The last time this library was modified.
	 */
	public abstract long getLastModified();


	/**
	 * Returns the location of this library, as a string.  If this library
	 * is contained in a single jar file, this will be the full path to that
	 * jar.  If it is a directory containing classes, it will be the full path
	 * of the directory.  Otherwise, this value will be <code>null</code>.
	 *
	 * @return The location of this library.
	 */
	public abstract String getLocationAsString();


	/**
	 * Returns information on the JRE running this application.  This will be
	 * <tt>rt.jar</tt> everywhere except OS X, where it will be
	 * <tt>classes.jar</tt>.  The associated source zip/jar file is also
	 * checked for.
	 *
	 * @return The information, or <code>null</code> if an error occurs.
	 * @see #getJreJarInfo(File)
	 */
	public static LibraryInfo getMainJreJarInfo() {
		String javaHome = System.getProperty("java.home");
		LibraryInfo info = getJreJarInfo(new File(javaHome));
		if (info != null) return info;
		// Fallback to bundled jmods in resources (for jlink images without jmods)
		LibraryInfo bundled = getBundledJmodsInfo();
		if (bundled != null) return bundled;
		return null;
	}

	// ---------------------------------------------------------------------
	// Bundled jmods handling (resources: com/deckerpw/flateditor/jdk/jmods/*.jmod)
	// ---------------------------------------------------------------------

	private static final String BUNDLED_JMODS_BASE = "com/deckerpw/flateditor/jdk/jmods";
	private static final String BUNDLED_JMODS_BASE_SLASH = BUNDLED_JMODS_BASE + "/";
	private static final String BUNDLED_JMODS_ZIP = "com/deckerpw/flateditor/jdk/jmods.zip";

	private static File bundledCacheDir = null;
	private static final Object bundledLock = new Object();

	/**
	 * Returns LibraryInfo for the bundled jmods in resources, extracting them
	 * to a cache directory if necessary. Shows a small modal progress dialog
	 * when extraction is required and called from the EDT.
	 * Returns null if bundled resources are not available or extraction fails.
	 */
	public static LibraryInfo getBundledJmodsInfo() {
		synchronized (bundledLock) {
			List<String> resourcePaths = listBundledJmodResources();
			if (resourcePaths.isEmpty()) {
				// Try zip fallback
				URL zipUrl = LibraryInfo.class.getClassLoader().getResource(BUNDLED_JMODS_ZIP);
				if (zipUrl == null) zipUrl = Thread.currentThread().getContextClassLoader().getResource(BUNDLED_JMODS_ZIP);
				if (zipUrl != null) {
					return getBundledFromZip(zipUrl);
				}
				LOG.log(Logger.Level.WARNING, "No bundled jmods found in resources: " + BUNDLED_JMODS_BASE);
				return null;
			}
			File cacheDir = getBundledCacheDir();
			if (isBundledCacheValid(cacheDir, resourcePaths)) {
				File[] files = cacheDir.listFiles(f -> f.getName().endsWith(".jmod"));
				if (files != null && files.length > 0) {
					return new Jdk9LibraryInfo(files);
				}
			}
			boolean ok = ensureBundledCache(cacheDir, resourcePaths);
			if (!ok) return null;
			File[] files = cacheDir.listFiles(f -> f.getName().endsWith(".jmod"));
			if (files == null || files.length == 0) return null;
			return new Jdk9LibraryInfo(files);
		}
	}

	private static LibraryInfo getBundledFromZip(URL zipUrl) {
		File cacheDir = getBundledCacheDir();
		// Check if cache already has jmods (from previous zip extraction)
		File[] existing = cacheDir.listFiles(f -> f.getName().endsWith(".jmod"));
		if (existing != null && existing.length >= 10) {
			File base = new File(cacheDir, "java.base.jmod");
			if (base.isFile() && base.length() > 0) return new Jdk9LibraryInfo(existing);
		}
		boolean ok = extractZipWithProgress(zipUrl, cacheDir);
		if (!ok) return null;
		File[] files = cacheDir.listFiles(f -> f.getName().endsWith(".jmod"));
		if (files == null || files.length == 0) return null;
		return new Jdk9LibraryInfo(files);
	}

	private static File getBundledCacheDir() {
		if (bundledCacheDir != null) return bundledCacheDir;
		String tmp = System.getProperty("java.io.tmpdir");
		// Use temp dir with app identifier; keep stable across runs
		File dir = new File(tmp, "FlatEditor/bundled-jmods");
		// Also try versioned dir to allow invalidation if needed
		bundledCacheDir = dir;
		return dir;
	}

	private static boolean isBundledCacheValid(File cacheDir, List<String> expectedResources) {
		if (!cacheDir.isDirectory()) return false;
		File sentinel = new File(cacheDir, ".ready");
		if (!sentinel.isFile()) return false;
		File base = new File(cacheDir, "java.base.jmod");
		if (!base.isFile() || base.length() == 0) return false;
		File[] files = cacheDir.listFiles(f -> f.getName().endsWith(".jmod"));
		if (files == null || files.length < expectedResources.size()) return false;
		// Check sentinel content matches expected count
		try {
			String content = Files.readString(sentinel.toPath()).trim();
			int count = Integer.parseInt(content);
			if (count != expectedResources.size()) return false;
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static List<String> listBundledJmodResources() {
		String base = BUNDLED_JMODS_BASE_SLASH;
		List<String> result = new ArrayList<>();
		// Try scanning codeSource (works for both jar and exploded)
		try {
			URL codeSourceUrl = LibraryInfo.class.getProtectionDomain().getCodeSource().getLocation();
			if (codeSourceUrl != null) {
				File codeSourceFile = new File(codeSourceUrl.toURI());
				if (codeSourceFile.isFile()) {
					try (JarFile jar = new JarFile(codeSourceFile)) {
						Enumeration<JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							JarEntry e = entries.nextElement();
							String name = e.getName();
							if (name.startsWith(base) && name.endsWith(".jmod") && !e.isDirectory()) {
								result.add(name);
							}
						}
					}
				} else if (codeSourceFile.isDirectory()) {
					File dir = new File(codeSourceFile, base);
					if (dir.isDirectory()) {
						File[] files = dir.listFiles((d, n) -> n.endsWith(".jmod"));
						if (files != null) for (File f : files) result.add(base + f.getName());
					}
				}
			}
		} catch (Exception e) {
			// ignore, fallback below
		}
		if (!result.isEmpty()) return result;

		// Fallback: classloader getResources for folder
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl == null) cl = LibraryInfo.class.getClassLoader();
			Enumeration<URL> urls = cl.getResources(BUNDLED_JMODS_BASE);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				if ("file".equals(url.getProtocol())) {
					File dir = new File(url.toURI());
					File[] files = dir.listFiles((d, n) -> n.endsWith(".jmod"));
					if (files != null) for (File f : files) {
						String path = base + f.getName();
						if (!result.contains(path)) result.add(path);
					}
				} else if ("jar".equals(url.getProtocol())) {
					String path = url.getPath();
					int fileIdx = path.indexOf("file:");
					int excl = path.indexOf("!");
					if (fileIdx >= 0 && excl > fileIdx) {
						String jarPathStr = path.substring(fileIdx, excl);
						File jarFile = new File(new URL(jarPathStr).toURI());
						try (JarFile jar = new JarFile(jarFile)) {
							Enumeration<JarEntry> entries = jar.entries();
							while (entries.hasMoreElements()) {
								JarEntry e = entries.nextElement();
								if (e.getName().startsWith(base) && e.getName().endsWith(".jmod")) {
									if (!result.contains(e.getName())) result.add(e.getName());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}
		if (!result.isEmpty()) return result;

		// Hardcoded fallback (JDK 21 - 70 modules)
		String[] hardcoded = {
			"java.base.jmod","java.compiler.jmod","java.datatransfer.jmod","java.desktop.jmod",
			"java.instrument.jmod","java.logging.jmod","java.management.jmod","java.management.rmi.jmod",
			"java.naming.jmod","java.net.http.jmod","java.prefs.jmod","java.rmi.jmod",
			"java.scripting.jmod","java.se.jmod","java.security.jgss.jmod","java.security.sasl.jmod",
			"java.smartcardio.jmod","java.sql.jmod","java.sql.rowset.jmod","java.transaction.xa.jmod",
			"java.xml.crypto.jmod","java.xml.jmod","jdk.accessibility.jmod","jdk.attach.jmod",
			"jdk.charsets.jmod","jdk.compiler.jmod","jdk.crypto.cryptoki.jmod","jdk.crypto.ec.jmod",
			"jdk.crypto.mscapi.jmod","jdk.dynalink.jmod","jdk.editpad.jmod","jdk.hotspot.agent.jmod",
			"jdk.httpserver.jmod","jdk.incubator.vector.jmod","jdk.internal.ed.jmod","jdk.internal.jvmstat.jmod",
			"jdk.internal.le.jmod","jdk.internal.opt.jmod","jdk.internal.vm.ci.jmod","jdk.internal.vm.compiler.jmod",
			"jdk.internal.vm.compiler.management.jmod","jdk.jartool.jmod","jdk.javadoc.jmod","jdk.jcmd.jmod",
			"jdk.jconsole.jmod","jdk.jdeps.jmod","jdk.jdi.jmod","jdk.jdwp.agent.jmod","jdk.jfr.jmod",
			"jdk.jlink.jmod","jdk.jpackage.jmod","jdk.jshell.jmod","jdk.jsobject.jmod","jdk.jstatd.jmod",
			"jdk.localedata.jmod","jdk.management.agent.jmod","jdk.management.jfr.jmod","jdk.management.jmod",
			"jdk.naming.dns.jmod","jdk.naming.rmi.jmod","jdk.net.jmod","jdk.nio.mapmode.jmod","jdk.random.jmod",
			"jdk.sctp.jmod","jdk.security.auth.jmod","jdk.security.jgss.jmod","jdk.unsupported.desktop.jmod",
			"jdk.unsupported.jmod","jdk.xml.dom.jmod","jdk.zipfs.jmod"
		};
		ClassLoader cl = LibraryInfo.class.getClassLoader();
		if (cl == null) cl = Thread.currentThread().getContextClassLoader();
		for (String n : hardcoded) {
			String full = base + n;
			boolean exists = false;
			if (cl != null && cl.getResource(full) != null) exists = true;
			if (!exists && LibraryInfo.class.getResource("/" + full) != null) exists = true;
			if (exists) result.add(full);
		}
		return result;
	}

	private static boolean ensureBundledCache(File cacheDir, List<String> resourcePaths) {
		// If headless (tests, no display), do quiet extraction
		if (java.awt.GraphicsEnvironment.isHeadless()) {
			return extractBundledQuiet(cacheDir, resourcePaths);
		}
		if (SwingUtilities.isEventDispatchThread()) {
			return extractBundledWithProgress(cacheDir, resourcePaths);
		}
		// Off EDT (e.g., called from main thread at startup) -> show dialog on EDT
		final boolean[] result = {false};
		final Exception[] holder = {null};
		try {
			SwingUtilities.invokeAndWait(() -> result[0] = extractBundledWithProgress(cacheDir, resourcePaths));
		} catch (Exception e) {
			holder[0] = e;
			e.printStackTrace();
		}
		if (holder[0] != null) {
			// fallback to quiet extraction if EDT dialog failed
			return extractBundledQuiet(cacheDir, resourcePaths);
		}
		return result[0];
	}

	private static boolean extractBundledQuiet(File cacheDir, List<String> resourcePaths) {
		try {
			if (!cacheDir.exists()) cacheDir.mkdirs();
			ClassLoader cl = LibraryInfo.class.getClassLoader();
			if (cl == null) cl = Thread.currentThread().getContextClassLoader();
			for (String res : resourcePaths) {
				String name = res.substring(res.lastIndexOf('/') + 1);
				File out = new File(cacheDir, name);
				try (InputStream in = cl.getResourceAsStream(res)) {
					if (in == null) {
						URL url = cl.getResource(res);
						if (url == null) continue;
						try (InputStream in2 = url.openStream()) {
							Files.copy(in2, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					} else {
						Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
			Files.writeString(new File(cacheDir, ".ready").toPath(), String.valueOf(resourcePaths.size()));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean extractBundledWithProgress(File cacheDir, List<String> resourcePaths) {
		JDialog dialog = new JDialog((JDialog)null, "Setting up JDK", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setResizable(false);
		JProgressBar progressBar = new JProgressBar(0, resourcePaths.size());
		progressBar.setStringPainted(true);
		progressBar.setString("Preparing...");
		progressBar.setValue(0);
		JLabel statusLabel = new JLabel("Extracting compiler modules, please wait...");
		JLabel fileLabel = new JLabel(" ");
		fileLabel.setFont(fileLabel.getFont().deriveFont(11f));
		JPanel content = new JPanel(new BorderLayout(10,10));
		content.setBorder(new EmptyBorder(15,15,15,15));
		content.add(statusLabel, BorderLayout.NORTH);
		content.add(progressBar, BorderLayout.CENTER);
		content.add(fileLabel, BorderLayout.SOUTH);
		dialog.setContentPane(content);
		dialog.setSize(420, 130);
		dialog.setLocationRelativeTo(null);

		AtomicBoolean success = new AtomicBoolean(false);
		final String[] errorMsg = new String[1];

		SwingWorker<Boolean,Void> worker = new SwingWorker<>() {
			@Override protected Boolean doInBackground() {
				try {
					if (!cacheDir.exists()) cacheDir.mkdirs();
					ClassLoader cl = LibraryInfo.class.getClassLoader();
					if (cl == null) cl = Thread.currentThread().getContextClassLoader();
					int total = resourcePaths.size();
					for (int i = 0; i < total; i++) {
						String res = resourcePaths.get(i);
						String name = res.substring(res.lastIndexOf('/') + 1);
						final String curName = name;
						final int idx = i + 1;
						SwingUtilities.invokeLater(() -> {
							fileLabel.setText(curName);
							statusLabel.setText("Extracting " + idx + " / " + total + " ...");
							progressBar.setValue(idx);
							progressBar.setString(idx + " / " + total);
						});
						File out = new File(cacheDir, name);
						// Use temp file then move for atomicity
						File tmpOut = new File(cacheDir, name + ".tmp");
						try (InputStream in = (cl != null ? cl.getResourceAsStream(res) : null)) {
							InputStream src = in;
							if (src == null) {
								URL url = (cl != null ? cl.getResource(res) : LibraryInfo.class.getResource("/" + res));
								if (url == null) throw new IOException("Resource not found: " + res);
								src = url.openStream();
							}
							try (InputStream s = src) {
								Files.copy(s, tmpOut.toPath(), StandardCopyOption.REPLACE_EXISTING);
							}
						}
						Files.move(tmpOut.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
						// Small yield to allow UI to repaint
						Thread.sleep(1);
					}
					Files.writeString(new File(cacheDir, ".ready").toPath(), String.valueOf(total));
					SwingUtilities.invokeLater(() -> fileLabel.setText("Done - " + total + " modules"));
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					errorMsg[0] = e.getMessage() != null ? e.getMessage() : e.toString();
					return false;
				}
			}
			@Override protected void done() {
				try { success.set(get()); } catch (Exception e) { errorMsg[0] = e.getMessage(); success.set(false); }
				finally { dialog.dispose(); }
			}
		};
		worker.execute();
		dialog.setVisible(true);
		try {
			boolean ok = worker.get();
			if (!ok) {
				String msg = errorMsg[0] != null ? errorMsg[0] : "Unknown error";
				JOptionPane.showMessageDialog(null, "Failed to extract bundled JDK modules to:\n" + cacheDir.getAbsolutePath() + "\n\n" + msg, "FlatEditor Setup Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Failed to extract bundled JDK modules:\n" + e.getMessage(), "FlatEditor Setup Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private static boolean extractZipWithProgress(URL zipUrl, File cacheDir) {
		JDialog dialog = new JDialog((JDialog)null, "Setting up JDK", Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setResizable(false);
		JProgressBar progressBar = new JProgressBar(0,100);
		progressBar.setStringPainted(true);
		progressBar.setString("Preparing...");
		progressBar.setIndeterminate(true);
		JLabel statusLabel = new JLabel("Extracting compiler modules, please wait...");
		JLabel fileLabel = new JLabel(" ");
		fileLabel.setFont(fileLabel.getFont().deriveFont(11f));
		JPanel content = new JPanel(new BorderLayout(10,10));
		content.setBorder(new EmptyBorder(15,15,15,15));
		content.add(statusLabel, BorderLayout.NORTH);
		content.add(progressBar, BorderLayout.CENTER);
		content.add(fileLabel, BorderLayout.SOUTH);
		dialog.setContentPane(content);
		dialog.setSize(420,130);
		dialog.setLocationRelativeTo(null);
		AtomicBoolean success = new AtomicBoolean(false);
		final String[] errorMsg = new String[1];
		SwingWorker<Boolean,Void> worker = new SwingWorker<>() {
			@Override protected Boolean doInBackground() {
				try {
					if (!cacheDir.exists()) cacheDir.mkdirs();
					long tmpBytes = -1;
					try { tmpBytes = zipUrl.openConnection().getContentLengthLong(); } catch (Exception ignored) {}
					final long totalBytes = tmpBytes;
					try (InputStream raw = zipUrl.openStream();
						 java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(raw)) {
						java.util.zip.ZipEntry entry;
						int count = 0;
						while ((entry = zis.getNextEntry()) != null) {
							String name = entry.getName();
							File out = new File(cacheDir, name);
							if (entry.isDirectory()) out.mkdirs();
							else {
								out.getParentFile().mkdirs();
								Files.copy(zis, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
								count++;
								final String cur = name;
								final int c = count;
								SwingUtilities.invokeLater(() -> {
									fileLabel.setText(cur);
									statusLabel.setText("Extracted " + c + " modules...");
								});
							}
							zis.closeEntry();
						}
						final int finalCount = count;
						SwingUtilities.invokeLater(() -> fileLabel.setText("Done - " + finalCount + " modules"));
					}
					// write sentinel with count
					File[] files = cacheDir.listFiles(f -> f.getName().endsWith(".jmod"));
					int n = files != null ? files.length : 0;
					Files.writeString(new File(cacheDir, ".ready").toPath(), String.valueOf(n));
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					errorMsg[0] = e.getMessage();
					return false;
				}
			}
			@Override protected void done() {
				try { success.set(get()); } catch (Exception e) { errorMsg[0]=e.getMessage(); success.set(false); }
				finally { dialog.dispose(); }
			}
		};
		worker.execute();
		dialog.setVisible(true);
		try {
			boolean ok = worker.get();
			if (!ok) {
				JOptionPane.showMessageDialog(null, "Failed to extract bundled JDK modules:\n" + (errorMsg[0]!=null?errorMsg[0]:"unknown"), "FlatEditor Setup Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Failed to extract bundled JDK modules:\n" + e.getMessage(), "FlatEditor Setup Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}


	/**
	 * Returns the location of the source corresponding to this library.
	 *
	 * @return The source for this library, or <code>null</code> if none.
	 * @see #setSourceLocation(SourceLocation)
	 */
	public SourceLocation getSourceLocation() {
		return sourceLoc;
	}


	@Override
	public int hashCode() {
		return hashCodeImpl();
	}


	/**
	 * Subclasses should override this method since {@link #equals(Object)} is
	 * overridden.  Instances of <code>LibraryInfo</code> aren't typically
	 * stored in maps, so the hash value isn't necessarily important to
	 * <code>RSTALanguageSupport</code>.
	 *
	 * @return The hash code for this library.
	 */
	public abstract int hashCodeImpl();


	/**
	 * Sets the location of the source corresponding to this library.
	 *
	 * @param sourceLoc The source location.  This may be <code>null</code>.
	 * @see #getSourceLocation()
	 */
	public void setSourceLocation(SourceLocation sourceLoc) {
		this.sourceLoc = sourceLoc;
	}


}
