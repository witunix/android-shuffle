package org.dodgybits.shuffle.android.core.model.protocol;

import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.dto.ShuffleProtos.Project.Builder;

public class ProjectProtocolTranslator implements ProtocolTranslator<Project, org.dodgybits.shuffle.dto.ShuffleProtos.Project> {

    private Locator<Context> mContextLocator;
    
    public ProjectProtocolTranslator(Locator<Context> contextLocator) {
        mContextLocator = contextLocator;
    }
    
    public org.dodgybits.shuffle.dto.ShuffleProtos.Project toMessage(Project project) {
        Builder builder = org.dodgybits.shuffle.dto.ShuffleProtos.Project.newBuilder();
        builder
            .setId(project.getLocalId().getId())
            .setName((project.getName()))
            .setModified(ProtocolUtil.toDate(project.getModifiedDate()))
            .setParallel(project.isParallel());
        
        final Id defaultContextId = project.getDefaultContextId();
        if (defaultContextId.isInitialised()) {
            builder.setDefaultContextId(defaultContextId.getId());
        }

        final Id tracksId = project.getTracksId();
        if (tracksId.isInitialised()) {
            builder.setTracksId(tracksId.getId());
        }
        
        return builder.build();
    }

    public Project fromMessage(
            org.dodgybits.shuffle.dto.ShuffleProtos.Project dto) {
        Project.Builder builder = Project.newBuilder();
        builder
            .setLocalId(Id.create(dto.getId()))
            .setName(dto.getName())
            .setModifiedDate(ProtocolUtil.fromDate(dto.getModified()))
            .setParallel(dto.getParallel());
            
        if (dto.hasDefaultContextId()) {
            Context context = mContextLocator.findById(dto.getDefaultContextId());
            builder.setDefaultContextId(context.getLocalId());
        }

        if (dto.hasTracksId()) {
            builder.setTracksId(Id.create(dto.getTracksId()));
        }

        return builder.build();
    }      
    
}