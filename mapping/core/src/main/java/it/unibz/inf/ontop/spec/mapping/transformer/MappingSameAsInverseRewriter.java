package it.unibz.inf.ontop.spec.mapping.transformer;

import it.unibz.inf.ontop.dbschema.DBMetadata;
import it.unibz.inf.ontop.spec.mapping.Mapping;

public interface MappingSameAsInverseRewriter {

    Mapping rewrite(Mapping mapping, DBMetadata dbMetadata);
}
