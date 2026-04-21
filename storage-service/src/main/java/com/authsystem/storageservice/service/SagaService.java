package com.authsystem.storageservice.service;

import com.authsystem.storageservice.entity.StorageSaga;
import com.authsystem.storageservice.entity.enums.SagaStatus;
import com.authsystem.storageservice.entity.enums.SagaType;
import com.authsystem.storageservice.repository.StorageSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaService {

    private final StorageSagaRepository storageSagaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StorageSaga startSaga(
            SagaType sagaType,
            String aggregateId,
            String currentStep
    ) {
        StorageSaga saga = StorageSaga.builder()
                .sagaType(sagaType)
                .aggregateId(aggregateId)
                .status(SagaStatus.STARTED)
                .currentStep(currentStep)
                .compensationRequired(false)
                .build();

        StorageSaga savedSaga = storageSagaRepository.save(saga);

        log.info(
                "ST_LOG | ACTION:SAGA_STARTED | SAGA_ID:{} | TYPE:{} | AGGREGATE_ID:{} | STEP:{}",
                savedSaga.getId(),
                sagaType,
                aggregateId,
                currentStep
        );

        return savedSaga;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(UUID sagaId, String currentStep) {
        StorageSaga saga = findSaga(sagaId);
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCurrentStep(currentStep);
        saga.setCompensationRequired(false);
        saga.setErrorMessage(null);
        storageSagaRepository.save(saga);

        log.info(
                "ST_LOG | ACTION:SAGA_COMPLETED | SAGA_ID:{} | STEP:{}",
                sagaId,
                currentStep
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            UUID sagaId,
            String currentStep,
            String errorMessage,
            boolean compensationRequired
    ) {
        StorageSaga saga = findSaga(sagaId);
        saga.setStatus(SagaStatus.FAILED);
        saga.setCurrentStep(currentStep);
        saga.setCompensationRequired(compensationRequired);
        saga.setErrorMessage(errorMessage);
        storageSagaRepository.save(saga);

        log.error(
                "ST_LOG | ACTION:SAGA_FAILED | SAGA_ID:{} | STEP:{} | COMPENSATION_REQUIRED:{} | MSG:{}",
                sagaId,
                currentStep,
                compensationRequired,
                errorMessage
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensated(UUID sagaId, String currentStep) {
        StorageSaga saga = findSaga(sagaId);
        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCurrentStep(currentStep);
        saga.setCompensationRequired(false);
        storageSagaRepository.save(saga);

        log.info(
                "ST_LOG | ACTION:SAGA_COMPENSATED | SAGA_ID:{} | STEP:{}",
                sagaId,
                currentStep
        );
    }

    private StorageSaga findSaga(UUID sagaId) {
        return storageSagaRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga not found"));
    }
}
