package com.optum.ocr.service;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.collections.keyvalue.DefaultKeyValue;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@EnableScheduling
public class PushService {
    private static KeyValue keyValue;
    final static DateFormat DATE_FORMATTER = new SimpleDateFormat("hh:mm:ss");
    final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public static String notificationTime() {
        String str = DATE_FORMATTER.format(new Date());
        return str;
    }

    public static void broadcast(String code, String message) {
        keyValue = new DefaultKeyValue(code, message);
    }

    public void addEmitter(final SseEmitter emitter) {
        emitters.add(emitter);
    }

    public void removeEmitter(final SseEmitter emitter) {
        emitters.remove(emitter);
    }

    @Async
    @Scheduled(fixedRate = 10000)
    public void doNotify() throws IOException {
        if (keyValue != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().data(keyValue));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            });
            emitters.removeAll(deadEmitters);
            keyValue = null;
        }
    }
}
