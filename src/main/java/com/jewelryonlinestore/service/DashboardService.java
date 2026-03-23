package com.jewelryonlinestore.service;

import com.jewelryonlinestore.dto.response.DashboardResponse;
import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse getDashboard(LocalDate from, LocalDate to);
}
