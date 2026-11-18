package com.lightos.iv.agent.tools

import com.lightos.iv.agent.Tool

/** The full set of capabilities the agent can call. */
fun allTools(): List<Tool> = listOf(
    ListInstalledAppsTool(),
    SearchAppsTool(),
    OpenAppTool(),
    SetTimerTool(),
    TakeNoteTool(),
    ListRecentNotesTool(),
    GetScheduleTodayTool(),
    OpenDialerTool(),
    MakeDirectCallTool(),
    SendSmsTool(),
    RouteToTool(),
    SearchWebTool(),
    ToggleTorchTool(),
    GetDeviceStatusTool(),
    SetSurfaceTool(),
    ReadAloudTool(),
)
