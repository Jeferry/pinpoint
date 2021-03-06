/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.web.controller;


import java.util.List;

import com.navercorp.pinpoint.common.server.bo.stat.ActiveTraceBo;
import com.navercorp.pinpoint.common.server.bo.stat.AgentStatDataPoint;
import com.navercorp.pinpoint.common.server.bo.stat.CpuLoadBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcDetailedBo;
import com.navercorp.pinpoint.common.server.bo.stat.TransactionBo;
import com.navercorp.pinpoint.web.service.stat.ActiveTraceChartService;
import com.navercorp.pinpoint.web.service.stat.AgentStatChartService;
import com.navercorp.pinpoint.web.service.stat.CpuLoadChartService;
import com.navercorp.pinpoint.web.service.stat.JvmGcChartService;
import com.navercorp.pinpoint.web.service.stat.JvmGcDetailedChartService;
import com.navercorp.pinpoint.web.service.stat.JvmGcService;
import com.navercorp.pinpoint.web.service.stat.TransactionChartService;
import com.navercorp.pinpoint.web.util.TimeWindowSampler;
import com.navercorp.pinpoint.web.vo.stat.chart.AgentStatChartGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.navercorp.pinpoint.web.service.stat.AgentStatService;
import com.navercorp.pinpoint.web.util.TimeWindow;
import com.navercorp.pinpoint.web.util.TimeWindowSlotCentricSampler;
import com.navercorp.pinpoint.web.vo.Range;

/**
 * @author emeroad
 * @author minwoo.jung
 * @author HyunGil Jeong
 */
public abstract class AgentStatController<T extends AgentStatDataPoint> {

    private final AgentStatService<T> agentStatService;

    private final AgentStatChartService agentStatChartService;

    public AgentStatController(AgentStatService<T> agentStatService, AgentStatChartService agentStatChartService) {
        this.agentStatService = agentStatService;
        this.agentStatChartService = agentStatChartService;
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<T> getAgentStat(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        Range rangeToScan = new Range(from, to);
        return this.agentStatService.selectAgentStatList(agentId, rangeToScan);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @RequestMapping(value = "/chart", method = RequestMethod.GET)
    @ResponseBody
    public AgentStatChartGroup getAgentStatChart(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        TimeWindowSampler sampler = new TimeWindowSlotCentricSampler();
        TimeWindow timeWindow = new TimeWindow(new Range(from, to), sampler);
        return this.agentStatChartService.selectAgentChart(agentId, timeWindow);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @RequestMapping(value = "/chart", method = RequestMethod.GET, params = {"interval"})
    @ResponseBody
    public AgentStatChartGroup getAgentStatChart(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to,
            @RequestParam("interval") Integer interval) {
        final int minSamplingInterval = 5;
        final long intervalMs = interval < minSamplingInterval ? minSamplingInterval * 1000L : interval * 1000L;
        TimeWindowSampler sampler = new TimeWindowSampler() {
            @Override
            public long getWindowSize(Range range) {
                return intervalMs;
            }
        };
        TimeWindow timeWindow = new TimeWindow(new Range(from, to), sampler);
        return this.agentStatChartService.selectAgentChart(agentId, timeWindow);
    }

    @Controller
    @RequestMapping("/getAgentStat/jvmGc")
    public static class JvmGcController extends AgentStatController<JvmGcBo> {
        @Autowired
        public JvmGcController(JvmGcService jvmGcService, JvmGcChartService jvmGcChartService) {
            super(jvmGcService, jvmGcChartService);
        }
    }

    @Controller
    @RequestMapping("/getAgentStat/jvmGcDetailed")
    public static class JvmGcDetailedController extends AgentStatController<JvmGcDetailedBo> {
        @Autowired
        public JvmGcDetailedController(AgentStatService<JvmGcDetailedBo> jvmGcDetailedService, JvmGcDetailedChartService jvmGcDetailedChartService) {
            super(jvmGcDetailedService, jvmGcDetailedChartService);
        }
    }

    @Controller
    @RequestMapping("/getAgentStat/cpuLoad")
    public static class CpuLoadController extends AgentStatController<CpuLoadBo> {
        @Autowired
        public CpuLoadController(AgentStatService<CpuLoadBo> cpuLoadService, CpuLoadChartService cpuLoadChartService) {
            super(cpuLoadService, cpuLoadChartService);
        }
    }

    @Controller
    @RequestMapping("/getAgentStat/transaction")
    public static class TransactionController extends AgentStatController<TransactionBo> {
        @Autowired
        public TransactionController(AgentStatService<TransactionBo> transactionService, TransactionChartService transactionChartService) {
            super(transactionService, transactionChartService);
        }
    }

    @Controller
    @RequestMapping("/getAgentStat/activeTrace")
    public static class ActiveTraceController extends AgentStatController<ActiveTraceBo> {
        @Autowired
        public ActiveTraceController(AgentStatService<ActiveTraceBo> activeTraceService, ActiveTraceChartService activeTraceChartService) {
            super(activeTraceService, activeTraceChartService);
        }
    }
}
