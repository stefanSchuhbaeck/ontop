package it.unibz.inf.ontop.spec.dbschema.impl;


import it.unibz.inf.ontop.dbschema.DBMetadata;
import it.unibz.inf.ontop.dbschema.RDBMetadata;
import it.unibz.inf.ontop.dbschema.RDBMetadataExtractionTools;
import it.unibz.inf.ontop.dbschema.RelationID;
import it.unibz.inf.ontop.exception.DBMetadataExtractionException;
import it.unibz.inf.ontop.injection.OntopMappingSQLSettings;
import it.unibz.inf.ontop.spec.dbschema.PreProcessedImplicitRelationalDBConstraintExtractor;
import it.unibz.inf.ontop.spec.dbschema.PreProcessedImplicitRelationalDBConstraintSet;
import it.unibz.inf.ontop.spec.dbschema.RDBMetadataExtractor;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMapping;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import static it.unibz.inf.ontop.spec.dbschema.impl.SQLTableNameExtractor.getRealTables;

/**
 * DBMetadataExtractor for JDBC-enabled DBs.
 */
public class DefaultRDBMetadataExtractor implements RDBMetadataExtractor {

    /**
     * If we have to parse the full metadata or just the table list in the mappings.
     */
    private final Boolean obtainFullMetadata;

    /**
     * This represents user-supplied constraints, i.e. primary
     * and foreign keys not present in the database metadata
     *
     * Can be useful for eliminating self-joins
     */
    private final PreProcessedImplicitRelationalDBConstraintExtractor implicitDBConstraintExtractor;

    @Inject
    private DefaultRDBMetadataExtractor(OntopMappingSQLSettings settings,
                                        PreProcessedImplicitRelationalDBConstraintExtractor implicitDBConstraintExtractor) {
        this.obtainFullMetadata = settings.isFullMetadataExtractionEnabled();
        this.implicitDBConstraintExtractor = implicitDBConstraintExtractor;
    }

    @Override
    public RDBMetadata extract(SQLPPMapping ppMapping, Connection connection, Optional<File> constraintFile)
            throws DBMetadataExtractionException {
        try {
            RDBMetadata metadata = RDBMetadataExtractionTools.createMetadata(connection);
            return extract(ppMapping, connection, metadata, constraintFile);
        } catch (SQLException e) {
            throw new DBMetadataExtractionException(e.getMessage());
        }
    }

    @Override
    public RDBMetadata extract(SQLPPMapping ppMapping, @Nullable Connection connection,
                               DBMetadata partiallyDefinedMetadata, Optional<File> constraintFile)
            throws DBMetadataExtractionException {

        if (!(partiallyDefinedMetadata instanceof RDBMetadata)) {
            throw new IllegalArgumentException("Was expecting a DBMetadata");
        }

        Optional<PreProcessedImplicitRelationalDBConstraintSet> implicitConstraints = constraintFile.isPresent()
                ? Optional.of(implicitDBConstraintExtractor.extract(constraintFile.get()))
                : Optional.empty();

        try {
            RDBMetadata metadata = (RDBMetadata) partiallyDefinedMetadata;

            // if we have to parse the full metadata or just the table list in the mappings
            if (obtainFullMetadata) {
                RDBMetadataExtractionTools.loadMetadata(metadata, connection, null);
            }
            else {
                try {
                    // This is the NEW way of obtaining part of the metadata
                    // (the schema.table names) by parsing the mappings

                    // Parse mappings. Just to get the table names in use

                    Set<RelationID> realTables = getRealTables(metadata.getQuotedIDFactory(), ppMapping.getTripleMaps());
                    implicitConstraints.ifPresent(c -> {
                        // Add the tables referred to by user-supplied foreign keys
                        Set<RelationID> referredTables = c.getReferredTables(metadata.getQuotedIDFactory());
                        realTables.addAll(referredTables);
                    });

                    RDBMetadataExtractionTools.loadMetadata(metadata, connection, realTables);
                }

                catch (SQLException e) {
                    System.out.println("Error obtaining the metadata " + e);
                }
            }

            implicitConstraints.ifPresent(c ->  {
                c.insertUniqueConstraints(metadata);
                c.insertForeignKeyConstraints(metadata);
            });

            return metadata;

        } catch (SQLException e) {
            throw new DBMetadataExtractionException(e.getMessage());
        }    }
}
