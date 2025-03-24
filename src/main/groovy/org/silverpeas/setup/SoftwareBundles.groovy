package org.silverpeas.setup

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles

import javax.inject.Inject

/**
 * Collections of software bundles required to construct the Silverpeas application.These bundles
 * will be downloaded from our software repository server (provided by our Nexus service) and then
 * unpacked to a given directory in order to generate the final application.
 * @author mmoquillon
 */
@SuppressWarnings('unused')
class SoftwareBundles {

    /**
     * All the software bundles that made Silverpeas. Those bundles are usually downloaded from our
     * own Software Repository by the Silverpeas installer. They are required to assemble and build
     * the final Silverpeas Web Application. The Jar libraries other than the supported JDBC drivers
     * aren't taken in charge.
     */
    @InputFiles
    final ConfigurableFileCollection silverpeas

    /**
     * Any tiers bundles to add into the Silverpeas Application being built. The tiers bundles are
     * processed differently by the plugin: only the JAR libraries are taken in charge.
     */
    @InputFiles
    final ConfigurableFileCollection tiers

    /**
     * Custom software bundles to complete or to add custom functionalities to Silverpeas. Those
     * bundles are usually downloaded from our own or outer Software Repositories by the
     * Silverpeas installer. The external repositories require to be declared. This is a way to
     * include libraries dedicated to implement a customer requirement (like a workflow process for
     * example).
     */
    @InputFiles
    final ConfigurableFileCollection custom

    @Inject
    SoftwareBundles(Project project) {
        silverpeas = project.files()
        tiers = project.files()
        custom = project.files()
    }

    void setSilverpeas(FileCollection bundles) {
        this.silverpeas.setFrom(bundles)
    }

    void setTiers(FileCollection bundles) {
        this.tiers.setFrom(bundles)
    }

    void setCustom(FileCollection bundles) {
        this.custom.setFrom(bundles)
    }
}
