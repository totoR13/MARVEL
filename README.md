## ❱ MARVEL

MARVEL (<u>M</u>obile-app <u>A</u>nti-<u>R</u>epackaging for <u>V</u>irtual <u>E</u>nvironments <u>L</u>ocking) is an anti-repackaging protection scheme that leverages the virtualization technique to mitigate traditional and virtualization-based repackaging attacks.  

This repository contains the implementation of MARVEL that consists of:

* **Trusted Container**, a virtualization app that extends the [VirtualApp](https://github.com/asLody/VirtualApp) framework and is responsible for the enforcement of the MARVEL runtime protection.

* **MARVELoid**, a Java tool that implements the MARVEL protection scheme for Android apps. The tool protects a plugin app by using code splitting and Interconnected Anti-Tampering Control (IAT). 
Code splitting allows to remove portions of code from the original app, thus introducing mitigation against static analysis inspection.
IATs involve the injection of integrity controls, evaluated during the interaction between the Trusted Container and a plugin app.

## ❱ Repository structure

The repo contains the following folders:

* `Binaries` - the executable files of MARVELoid (i.e., a jar file) and of the Trusted Container app (i.e., an APK file);
* `Docker` - a docker image to run the MARVELoid protection process on a set of apks;
* `Example` - an example of the MARVELoid protection process with instructions for reproducing it;
* `Experiments` - the details concerning the test-set used in our experimental campaign and the results of the protection;
* `Sources` - the source code of the MARVELoid tool and of the Trusted Container Android app.

For more details, please refer to the `READMEs` in the specific folders.

## ❱ License

This tool is available under a dual license: a commercial one required for closed source projects or commercial projects, and an AGPL license for open-source projects.

Depending on your needs, you must choose one of them and follow its policies. A detail of the policies and agreements for each license type is available in the [LICENSE.COMMERCIAL](LICENSE.COMMERCIAL) and [LICENSE](LICENSE) files.

## ❱ Credits
[![Unige](https://intranet.dibris.unige.it/img/logo_unige.gif)](https://unige.it/en/)
[![Dibris](https://intranet.dibris.unige.it/img/logo_dibris.gif)](https://www.dibris.unige.it/en/)

[![Unipd](https://altheiascience.com/wp-content/uploads/2019/10/logo-unipd.png)](https://www.unipd.it/)

## ❱ Team 

* [Antonio Ruggia](https://github.com/totoR13) - PhD. Student
* [Eleonora Losiouk](https://www.math.unipd.it/~elosiouk/) - Assistant Professor
* [Luca Verderame](https://csec.it/people/luca_verderame/) - Postdoctoral Researcher
* [Mauro Conti](https://www.math.unipd.it/~conti/) - Full Professor
* [Alessio Merlo](https://csec.it/people/alessio_merlo/) - Associate Professor
