package com.foretell.sportsmeetings.util.scheduler;

import com.foretell.sportsmeetings.model.Meeting;
import com.foretell.sportsmeetings.model.MeetingStatus;
import com.foretell.sportsmeetings.repo.MeetingRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class CustomScheduler {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final MeetingRepo meetingRepo;

    public CustomScheduler(ThreadPoolTaskScheduler taskScheduler, MeetingRepo meetingRepo) {
        this.taskScheduler = taskScheduler;
        this.meetingRepo = meetingRepo;
    }

    public void scheduleAllMeetingsFinishedStatusTask() {
        List<Meeting> meetings = meetingRepo.findAllByStatus(MeetingStatus.CREATED.toString());
        meetings.forEach(meeting -> scheduleMeetingFinishedStatusTask(meeting.getId(), meeting.getEndDate().getTime()));
    }

    public void scheduleMeetingFinishedStatusTask(Long meetingId, Date endDate) {
        taskScheduler.schedule(
                () -> {
                    Optional<Meeting> maybeMeeting = meetingRepo.findById(meetingId);
                    if (maybeMeeting.isPresent()) {
                        Meeting meetingFromDb = maybeMeeting.get();
                        meetingFromDb.setStatus(MeetingStatus.FINISHED);
                        meetingRepo.save(meetingFromDb);
                        meetingFromDb.getParticipants().forEach(user -> {
                            Long chatId = user.getTelegramBotChatId();
                        });
                        log.info("Status of meeting with id: " + (meetingId) + " set to FINISHED");
                    } else {
                        log.error("Meeting with id: " + (meetingId) + " not found");
                    }
                },

                endDate
        );
    }

    public void scheduleTelegramStartMeetingNotification(Long meetingId, Long notificationTimestamp) {
        Date date = new Date();
        date.setTime(notificationTimestamp);
        taskScheduler.schedule(
                () -> {
                    Optional<Meeting> maybeMeeting = meetingRepo.findById(meetingId);
                    if (maybeMeeting.isPresent()) {
                        Meeting meeting = maybeMeeting.get();
                        meeting.getParticipants().forEach(user -> {
                            Long chatId = user.getTelegramBotChatId();
                        });
                    }
                },
                date
        );
    }

}
