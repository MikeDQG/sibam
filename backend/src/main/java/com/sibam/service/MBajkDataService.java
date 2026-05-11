package com.sibam.service;

import com.sibam.dto.mbajk.BikeStopDto;
import com.sibam.integration.mbajk.MBajkClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MBajkDataService {
    private final MBajkClient mbajkClient;
    public MBajkDataService(MBajkClient mbajkClient) {
        this.mbajkClient = mbajkClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testBikesIngestion() {
        mbajkClient.getAllBikes().subscribe(response -> {
            System.out.println("--- ŠibaM: Začenjam testni prevzem podatkov koles ---");
            List<BikeStopDto> bikeStops = response;

            System.out.println(bikeStops.size());

            bikeStops.stream().limit(3).forEach(System.out::println);
        });
    }
}
