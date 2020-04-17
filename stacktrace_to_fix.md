
### Stacktraces to fix:
```
04-12 16:55:33.024 E/lmkd    (  902): Couldn't get Swap info. Is it kthread?
04-12 16:55:33.024 E/lmkd    (  902): Couldn't get Swap info. Is it kthread?
04-12 16:55:33.129 E/Zygote  (17758): isWhitelistProcess - Process is Whitelisted
04-12 16:55:33.132 E/Zygote  (17758): accessInfo : 1
04-12 16:55:33.160 E/mm.embms:remot(17758): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.205 E/Zygote  (17782): accessInfo : 2
04-12 16:55:33.206 E/Zygote  (17782): setSDPgroupsIntarray[pid]: 0 / [gid]: 15010414 / [uid]: 15010414
04-12 16:55:33.257 E/Zygote  (17798): accessInfo : 2
04-12 16:55:33.257 E/Zygote  (17798): setSDPgroupsIntarray[pid]: 0 / [gid]: 15001001 / [uid]: 15001001
04-12 16:55:33.279 E/mhelper_servic(17782): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.296 E/m.android.phon(17798): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.348 E/Zygote  (17828): accessInfo : 2
04-12 16:55:33.348 E/Zygote  (17828): setSDPgroupsIntarray[pid]: 0 / [gid]: 15010187 / [uid]: 15010187
04-12 16:55:33.367 E/mm.embms:remot(17828): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.408 E/audit   (  601): avc:  denied  { add } for interface=vendor.qti.hardware.systemhelper::ISystemEvent sid=u:r:platform_app:s0:c662,c768 pid=17782 scontext=u:r:platform_app:s0:c662,c768 tcontext=u:object_r:default_android_hwservice:s0 tclass=hwservice_manager permissive=0
04-12 16:55:33.414 E/        (17782): Could not register SystemEvent 1.0 interface
04-12 16:55:33.414 E/SysHelperService(17782): failed to initialise service, exiting...
04-12 16:55:33.438 E/Zygote  (17855): isWhitelistProcess - Process is Whitelisted
04-12 16:55:33.441 E/Zygote  (17855): accessInfo : 1
04-12 16:55:33.469 E/android:drmSer(17855): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.474 E/NwkInfoProvider(17798): [DB Version] getVersion: 3211272 - DatabaseHelper
04-12 16:55:33.551 E/Zygote  (17898): isWhitelistProcess - Process is Whitelisted
04-12 16:55:33.553 E/Zygote  (17898): accessInfo : 1
04-12 16:55:33.583 E/mm.embms:remot(17898): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.622 E/Zygote  (17920): accessInfo : 2
04-12 16:55:33.622 E/Zygote  (17920): setSDPgroupsIntarray[pid]: 0 / [gid]: 15010414 / [uid]: 15010414
04-12 16:55:33.654 E/mhelper_servic(17920): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.661 E/Zygote  (17932): accessInfo : 2
04-12 16:55:33.661 E/Zygote  (17932): setSDPgroupsIntarray[pid]: 0 / [gid]: 15001001 / [uid]: 15001001
04-12 16:55:33.683 E/m.android.phon(17932): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.691 E/WifiService( 1332): 11791 has no permission about LOCAL_MAC_ADDRESS
04-12 16:55:33.693 E/Zygote  (17953): accessInfo : 2
04-12 16:55:33.693 E/Zygote  (17953): setSDPgroupsIntarray[pid]: 0 / [gid]: 15010187 / [uid]: 15010187
04-12 16:55:33.717 E/mm.embms:remot(17953): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.721 E/Zygote  (17981): isWhitelistProcess - Process is Whitelisted
04-12 16:55:33.726 E/Zygote  (17981): accessInfo : 1
04-12 16:55:33.755 E/android:drmSer(17981): Not starting debugger since process cannot load the jdwp agent.
04-12 16:55:33.760 E/InputDispatcher( 1332): channel '8e2e36a me.retrodaredevil.solarthing.android/me.retrodaredevil.solarthing.android.SettingsActivity (server)' ~ Channel is unrecoverably broken and will be disposed!
04-12 16:55:33.774 E/audit   (  601): avc:  denied  { add } for interface=vendor.qti.hardware.systemhelper::ISystemEvent sid=u:r:platform_app:s0:c662,c768 pid=17920 scontext=u:r:platform_app:s0:c662,c768 tcontext=u:object_r:default_android_hwservice:s0 tclass=hwservice_manager permissive=0
04-12 16:55:33.778 E/        (17920): Could not register SystemEvent 1.0 interface
04-12 16:55:33.779 E/SysHelperService(17920): failed to initialise service, exiting...
04-12 16:55:33.816 E/NwkInfoProvider(17932): [DB Version] getVersion: 3211272 - DatabaseHelper
04-12 16:55:33.943 E/pandora.androi(17600): Invalid ID 0x00000000.
04-12 16:55:33.961 E/WifiService( 1332): 11791 has no permission about LOCAL_MAC_ADDRESS
04-12 16:55:33.966 E/WifiService( 1332): 11791 has no permission about LOCAL_MAC_ADDRESS
04-12 16:55:34.303 E/GraphResponse(17600): {HttpStatus: 400, errorCode: 43003, subErrorCode: -1, errorType: OAuthException, errorMessage: (#43003) App button auto detection is disabled}
0
```

