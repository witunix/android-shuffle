package org.dodgybits.shuffle.android.core.model.encoding;

import static android.provider.BaseColumns._ID;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.ARCHIVED;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.DEFAULT_CONTEXT_ID;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.MODIFIED_DATE;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.NAME;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.PARALLEL;
import static org.dodgybits.shuffle.android.persistence.provider.ProjectProvider.Projects.TRACKS_ID;

import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Project.Builder;

import android.os.Bundle;

import com.google.inject.Singleton;

@Singleton
public class ProjectEncoder extends AbstractEntityEncoder implements EntityEncoder<Project> {

    @Override
    public void save(Bundle icicle, Project project) {
        putId(icicle, _ID, project.getLocalId());
        putId(icicle, TRACKS_ID, project.getTracksId());
        icicle.putLong(MODIFIED_DATE, project.getModifiedDate());

        putString(icicle, NAME, project.getName());
        putId(icicle, DEFAULT_CONTEXT_ID, project.getDefaultContextId());
        icicle.putBoolean(ARCHIVED, project.isArchived());
        icicle.putBoolean(PARALLEL, project.isParallel());
    }
    
    @Override
    public Project restore(Bundle icicle) {
        if (icicle == null) return null;

        Builder builder = Project.newBuilder();
        builder.setLocalId(getId(icicle, _ID));
        builder.setModifiedDate(icicle.getLong(MODIFIED_DATE, 0L));
        builder.setTracksId(getId(icicle, TRACKS_ID));

        builder.setName(getString(icicle, NAME));
        builder.setDefaultContextId(getId(icicle, DEFAULT_CONTEXT_ID));
        builder.setArchived(icicle.getBoolean(ARCHIVED));
        builder.setParallel(icicle.getBoolean(PARALLEL));
        
        return builder.build();
    }
    
}
