package utils;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.AxmlWriter;
import pxb.android.axml.NodeVisitor;
import soot.SourceLocator;
import soot.options.Options;

import java.io.*;
import java.nio.file.*;

public class ManifestHelper {
    private static String packageName = null;
    private static boolean firstAppRetrieve = true;
    private static String applicationClassName = null;

    /**
     * Return the package name specified in the AndroidManifest file of the input apk file
     *
     * @throws IllegalArgumentException if no package name is found
     */
    public static String getPackageName() {
        if (ManifestHelper.packageName != null) {
            return ManifestHelper.packageName;
        }

        String apkPath = Options.v().process_dir().stream().findFirst().orElseThrow(IllegalStateException::new);
        return getPackageName(apkPath);
    }

    /**
     * Return the package name specified in the AndroidManifest file of the apk file at input path
     *
     * @param apkPath The input apk path
     *
     * @throws IllegalArgumentException if no package name is found
     */
    public static String getPackageName(String apkPath) {
        final String[] packName = {null};
        try (FileSystem apk = FileSystems.newFileSystem(Paths.get(apkPath), null)) {
            Path mfPath = apk.getPath("/AndroidManifest.xml");
            AxmlReader r = new AxmlReader(Files.readAllBytes(mfPath));
            r.accept(new AxmlVisitor() {
                @Override
                public NodeVisitor child(String ns, String name) { // ns: null; name: manifest
                    return new NodeVisitor(super.child(ns, name)) {
                        @Override
                        public void attr(String ns, String name, int resourceId, int type, Object obj) {
                            if(name.equals("package"))
                                packName[0] = (String) obj;
                        }
                    };
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(packName[0] == null)
            throw new IllegalArgumentException("No package attribute supplied in 'manifest' XML element");

        ManifestHelper.packageName = packName[0];
        return packName[0];
    }

    /**
     * Get the path of the Application class specified in the Android manifest of the input apk
     *
     * @return the path of the class or null if does not exists
     */
    public static String getApplicationClassName() {
        if (!ManifestHelper.firstAppRetrieve) {
            return ManifestHelper.applicationClassName;
        }

        String apkPath = Options.v().process_dir().stream().findFirst().orElseThrow(IllegalStateException::new);
        final String[] extracted = {null};
        try (FileSystem apk = FileSystems.newFileSystem(Paths.get(apkPath), null)) {
            Path mfPath = apk.getPath("/AndroidManifest.xml");
            AxmlReader r = new AxmlReader(Files.readAllBytes(mfPath));
            r.accept(new AxmlVisitor() {
                @Override
                public NodeVisitor child(String ns, String name) { // ns: null; name: manifest
                    return new NodeVisitor(super.child(ns, name)) {
                        @Override
                        public NodeVisitor child(String ns, String name) {
                            if (name.equals("application")) {
                                return new NodeVisitor() {
                                    @Override
                                    public void attr(String ns, String name, int resourceId, int type, Object obj) {
                                        if(name.equals("name"))
                                            extracted[0] = (String) obj;
                                    }
                                };
                            }
                            return null;
                        }
                    };
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String appClassName = extracted[0];
        if (appClassName != null && appClassName.startsWith(".")) {
            // if appClassName starts with a dot, then prepend the package name
            appClassName = ManifestHelper.getPackageName() + appClassName;
        }

        ManifestHelper.firstAppRetrieve = false;
        ManifestHelper.applicationClassName = appClassName;
        return appClassName;
    }

    /**
     * This method override the android:name attribute of the application in AndroidManifest file.
     *
     * @param newApplicationPath the new application path
     */
    public static void updateApplicationClassName(String newApplicationPath, File apkFile) {
        // File apkFile = SourceLocator.v().dexClassIndex().values().stream().findFirst().orElseThrow(IllegalStateException::new);
        try (FileSystem apk = FileSystems.newFileSystem(Paths.get(SourceLocator.v().getOutputDir(), apkFile.getName()), null)) {
            Path mfPath = apk.getPath("/AndroidManifest.xml");
            AxmlReader reader = new AxmlReader(Files.readAllBytes(mfPath));
            AxmlWriter writer = new AxmlWriter();
            reader.accept(new AxmlVisitor(writer) {
                @Override
                public NodeVisitor child(String ns, String name) {
                    // return new MyNodeVisitor(super.child(ns, name), name);
                    return new NodeVisitor(super.child(ns, name)) {
                        @Override
                        public NodeVisitor child(String ns, String name) {
                            // check if exist
                            if (name.equals("application")) {
                                return new NodeVisitor(super.child(ns, name)) {
                                    private boolean found = false;

                                    @Override
                                    public void attr(String ns, String name, int resourceId, int type, Object obj) {

                                        if(name.equals("name")) {
                                            obj = newApplicationPath;
                                            found = true;
                                        }

                                        super.attr(ns, name, resourceId, type, obj);
                                    }

                                    @Override
                                    public void end() {
                                        if (!found) {
                                            super.attr("http://schemas.android.com/apk/res/android", "name", 16842755, 3, newApplicationPath);
                                        }

                                        if (nv != null) {
                                            nv.end();
                                        }
                                    }
                                };
                            }
                            return super.child(ns, name);
                        }
                    };
                }
            });

            Files.write(mfPath, writer.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}
