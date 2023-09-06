package org.avni.server.dao;

import org.avni.server.domain.Individual;
import org.avni.server.domain.User;
import org.avni.server.domain.UserSubjectAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "userSubjectAssignment", path = "userSubjectAssignment", exported = false)
public interface UserSubjectAssignmentRepository extends ReferenceDataRepository<UserSubjectAssignment> {
    UserSubjectAssignment findByUserAndSubjectAndIsVoidedFalse(User user, Individual subject);
    List<UserSubjectAssignment> findAllBySubjectAndIsVoidedFalse(Individual subject);

    List<UserSubjectAssignment> findUserSubjectAssignmentBySubject_IdIn(List<Long> subjectIds);
    List<UserSubjectAssignment> findUserSubjectAssignmentByUserIsNotAndSubject_IdIn(User user, List<Long> subjectIds);

    boolean existsByUserAndIsVoidedTrueAndLastModifiedDateTimeGreaterThan(User user, Date lastModifiedDateTime);

    Page<UserSubjectAssignment> findByUserAndIsVoidedTrueAndLastModifiedDateTimeIsBetweenOrderByLastModifiedDateTimeAscIdAsc(
            User user,
            Date lastModifiedDate,
            Date now,
            Pageable pageable
    );

    Slice<UserSubjectAssignment> findSliceByUserAndIsVoidedTrueAndLastModifiedDateTimeIsBetweenOrderByLastModifiedDateTimeAscIdAsc(
            User user,
            Date lastModifiedDate,
            Date now,
            Pageable pageable
    );

    default UserSubjectAssignment findByName(String name) {
        throw new UnsupportedOperationException("No field 'name' in UserSubjectAssignment.");
    }

    default UserSubjectAssignment findByNameIgnoreCase(String name) {
        throw new UnsupportedOperationException("No field 'name' in UserSubjectAssignment.");
    }

    List<UserSubjectAssignment> findByOrganisationId(Long organisationId);

    default UserSubjectAssignment saveUserSubjectAssignment(UserSubjectAssignment usa) {
        synchronized (String.format("USA-%d-%d", usa.getSubject().getId(), usa.getUser().getId()).intern()) {
            if (usa.isNew() && this.findByUserAndSubjectAndIsVoidedFalse(usa.getUser(), usa.getSubject()) != null)
                throw new RuntimeException(String.format("Another assignment with same subject %s and user %s exists.", usa.getSubject().getUuid(), usa.getUser().getUsername()));
            return this.save(usa);
        }
    }
}
