package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.ReadingProgress;
import stu.datn.ebook_store.entity.User;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.repository.ReadingProgressRepository;
import stu.datn.ebook_store.service.ReadingProgressService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReadingProgressServiceImpl implements ReadingProgressService {

    private final ReadingProgressRepository readingProgressRepository;

    @Autowired
    public ReadingProgressServiceImpl(ReadingProgressRepository readingProgressRepository) {
        this.readingProgressRepository = readingProgressRepository;
    }

    @Override
    public List<ReadingProgress> getAllReadingProgress() {
        return readingProgressRepository.findAll();
    }

    @Override
    public Optional<ReadingProgress> getReadingProgressById(String progressId) {
        return readingProgressRepository.findById(progressId);
    }

    @Override
    public Optional<ReadingProgress> getReadingProgressByUserAndBook(User user, Book book) {
        return readingProgressRepository.findByUserAndBook(user, book);
    }

    @Override
    public ReadingProgress saveReadingProgress(ReadingProgress readingProgress) {
        if (readingProgress.getProgressId() == null || readingProgress.getProgressId().isEmpty()) {
            readingProgress.setProgressId(generateProgressId());
        }
        return readingProgressRepository.save(readingProgress);
    }

    @Override
    public void deleteReadingProgress(String progressId) {
        readingProgressRepository.deleteById(progressId);
    }

    @Override
    public List<ReadingProgress> getReadingProgressByUser(User user) {
        return readingProgressRepository.findByUser(user);
    }

    @Override
    public List<ReadingProgress> getFavoriteBooksByUser(User user) {
        return readingProgressRepository.findByUserAndIsFavoriteTrue(user);
    }

    @Override
    public List<ReadingProgress> getRecentReadingByUser(User user) {
        return readingProgressRepository.findByUserOrderByLastReadAtDesc(user);
    }

    @Override
    public List<ReadingProgress> getCompletedBooksByUser(User user) {
        return readingProgressRepository.findByUserAndIsCompletedTrue(user);
    }

    @Override
    public List<ReadingProgress> getReadingProgressByUserAndAccessType(User user, ReadingProgress.AccessType accessType) {
        return readingProgressRepository.findByUserAndAccessType(user, accessType);
    }

    @Override
    public List<ReadingProgress> getReadingProgressByBook(Book book) {
        return readingProgressRepository.findByBook(book);
    }

    @Override
    public long countCompletedBooksByUser(User user) {
        return readingProgressRepository.countByUserAndIsCompletedTrue(user);
    }

    @Override
    public List<ReadingProgress> getContinueReadingByUser(User user) {
        return readingProgressRepository.findContinueReading(user);
    }

    @Override
    public void markAsFavorite(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setIsFavorite(true);
            readingProgressRepository.save(progress);
        }
    }

    @Override
    public void unmarkAsFavorite(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setIsFavorite(false);
            readingProgressRepository.save(progress);
        }
    }

    @Override
    public void markAsCompleted(String progressId) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setIsCompleted(true);
            progress.setProgressPercentage(100.0f);
            progress.setLastReadAt(LocalDateTime.now());
            readingProgressRepository.save(progress);
        }
    }

    @Override
    public void updateProgress(String progressId, Float percentage, String location) {
        Optional<ReadingProgress> progressOpt = readingProgressRepository.findById(progressId);
        if (progressOpt.isPresent()) {
            ReadingProgress progress = progressOpt.get();
            progress.setProgressPercentage(percentage);
            progress.setLastReadLocation(location);
            progress.setLastReadAt(LocalDateTime.now());
            if (percentage >= 100.0f) {
                progress.setIsCompleted(true);
            }
            readingProgressRepository.save(progress);
        }
    }

    private String generateProgressId() {
        long count = readingProgressRepository.count();
        return "progress_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}

