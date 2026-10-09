<#
The desktop of a hosted Windows runner for the showcase (tools/Cycle.java) : the screen, the window in front, the windows
with a title, a screenshot. CI only (it changes the display mode and minimizes every window). Best effort : every part
logs its failure and the script exits 0 ; the showcase reports the screen it got (report.json : screen).

-Prepare : the largest display mode up to 3840x2160 (the runners start at 1024x768), no screen saver, no foreground lock
time-out, no monitor or standby time-out. -Minimize : every window minimized (the agent terminal may cover the windows
of the showcase), right before a cycle. -Screenshot : a screenshot of the whole screen.
#>
param([switch] $Prepare, [switch] $Minimize, [string] $Screenshot)

function Step([string] $name, [scriptblock] $block) {
    try { & $block } catch { "${name} failed : $_" }
}

Step 'types' {
    Add-Type -AssemblyName System.Windows.Forms, System.Drawing
    Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

public static class ShowcaseDesktop {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    struct DEVMODE {
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)] public string dmDeviceName;
        public short dmSpecVersion, dmDriverVersion, dmSize, dmDriverExtra;
        public int dmFields, dmPositionX, dmPositionY, dmDisplayOrientation, dmDisplayFixedOutput;
        public short dmColor, dmDuplex, dmYResolution, dmTTOption, dmCollate;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)] public string dmFormName;
        public short dmLogPixels;
        public int dmBitsPerPel, dmPelsWidth, dmPelsHeight, dmDisplayFlags, dmDisplayFrequency, dmICMMethod, dmICMIntent,
            dmMediaType, dmDitherType, dmReserved1, dmReserved2, dmPanningWidth, dmPanningHeight;
    }

    delegate bool EnumWindowsProc(IntPtr hwnd, IntPtr param);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern bool EnumDisplaySettings(string device, int mode, ref DEVMODE dm);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern int ChangeDisplaySettings(ref DEVMODE dm, int flags);
    [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint pid);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern int GetWindowText(IntPtr hwnd, StringBuilder text, int max);
    [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr hwnd);
    [DllImport("user32.dll")] static extern bool EnumWindows(EnumWindowsProc proc, IntPtr param);
    [DllImport("user32.dll")] static extern bool PostMessage(IntPtr hwnd, int msg, IntPtr wparam, IntPtr lparam);
    [DllImport("user32.dll")] static extern bool SystemParametersInfo(int action, int param, IntPtr value, int flags);
    [DllImport("user32.dll")] static extern void keybd_event(byte vk, byte scan, int flags, UIntPtr extra);

    static DEVMODE NewMode() {
        DEVMODE dm = new DEVMODE();
        dm.dmSize = (short) Marshal.SizeOf(typeof(DEVMODE));
        return dm;
    }

    // the modes of the primary display, WxH
    public static string[] Modes() {
        List<string> modes = new List<string>();
        DEVMODE dm = NewMode();
        for (int i = 0; EnumDisplaySettings(null, i, ref dm); i++) {
            string mode = dm.dmPelsWidth + "x" + dm.dmPelsHeight;
            if (!modes.Contains(mode)) modes.Add(mode);
        }
        return modes.ToArray();
    }

    // the primary display in another mode, for this session : 0 (DISP_CHANGE_SUCCESSFUL) when it succeeded
    public static int SetMode(int width, int height) {
        DEVMODE dm = NewMode();
        if (!EnumDisplaySettings(null, -1, ref dm)) return -100;       // ENUM_CURRENT_SETTINGS
        dm.dmPelsWidth = width;
        dm.dmPelsHeight = height;
        dm.dmFields = 0x80000 | 0x100000;                               // DM_PELSWIDTH | DM_PELSHEIGHT
        return ChangeDisplaySettings(ref dm, 0);
    }

    static string Title(IntPtr hwnd) {
        StringBuilder title = new StringBuilder(256);
        GetWindowText(hwnd, title, title.Capacity);
        return title.ToString();
    }

    // the process id and the title of the foreground window
    public static string Foreground() {
        IntPtr hwnd = GetForegroundWindow();
        uint pid;
        GetWindowThreadProcessId(hwnd, out pid);
        return pid + " '" + Title(hwnd) + "'";
    }

    // WM_CLOSE to the visible top-level windows whose title contains the text : their number
    public static int Close(string text) {
        int closed = 0;
        EnumWindows(delegate (IntPtr hwnd, IntPtr param) {
            if (IsWindowVisible(hwnd) && Title(hwnd).IndexOf(text, StringComparison.OrdinalIgnoreCase) >= 0) {
                PostMessage(hwnd, 0x0010, IntPtr.Zero, IntPtr.Zero);   // WM_CLOSE
                closed++;
            }
            return true;
        }, IntPtr.Zero);
        return closed;
    }

    // SystemParametersInfo with the value 0 (SPIF_SENDCHANGE)
    public static bool SetZero(int action) {
        return SystemParametersInfo(action, 0, IntPtr.Zero, 2);
    }

    // a key press and release (a virtual key code)
    public static void Tap(byte vk) {
        keybd_event(vk, 0, 0, UIntPtr.Zero);
        keybd_event(vk, 0, 2, UIntPtr.Zero);                            // KEYEVENTF_KEYUP
    }
}
'@
}

function Show-Desktop([string] $when) {
    $size = [System.Windows.Forms.SystemInformation]::PrimaryMonitorSize
    $work = [System.Windows.Forms.SystemInformation]::WorkingArea
    "$when : screen $($size.Width)x$($size.Height), work area $($work.Width)x$($work.Height)"
    $foreground = [ShowcaseDesktop]::Foreground()
    $process = Get-Process -Id ([int] ($foreground -split ' ')[0]) -ErrorAction SilentlyContinue
    "foreground window : $($process.ProcessName) $foreground"
    Get-Process | Where-Object MainWindowTitle | Format-Table Id, ProcessName, MainWindowTitle -AutoSize | Out-String -Width 200
}

Step 'system' {
    [System.Environment]::OSVersion.VersionString
    "architecture : $env:PROCESSOR_ARCHITECTURE"
    query user 2>&1
}
Step 'desktop' { Show-Desktop 'before' }

if ($Prepare) {
    Step 'video' {
        Get-CimInstance Win32_VideoController | Format-Table Name, CurrentHorizontalResolution, CurrentVerticalResolution -AutoSize |
            Out-String -Width 200
    }
    Step 'display mode' {
        $modes = [ShowcaseDesktop]::Modes()
        "display modes : $($modes -join ' ')"
        $best = $modes | ForEach-Object { $w, $h = $_ -split 'x'; [pscustomobject] @{ W = [int] $w; H = [int] $h } } |
            Where-Object { $_.W -le 3840 -and $_.H -le 2160 } | Sort-Object { $_.W * $_.H } | Select-Object -Last 1
        if ($best) { "display mode $($best.W)x$($best.H) : $([ShowcaseDesktop]::SetMode($best.W, $best.H)) (0 : done)" }
    }
    Step 'screen saver' { "screen saver off : $([ShowcaseDesktop]::SetZero(0x0011))" }       # SPI_SETSCREENSAVEACTIVE
    Step 'foreground lock' {
        [ShowcaseDesktop]::Tap(0x10)                                                     # Shift : this process sent the last input
        "foreground lock time-out 0 : $([ShowcaseDesktop]::SetZero(0x2001))"               # SPI_SETFOREGROUNDLOCKTIMEOUT
    }
    Step 'power' {
        powercfg /change monitor-timeout-ac 0
        powercfg /change standby-timeout-ac 0
    }
}

if ($Prepare -or $Minimize) {
    Step 'minimize' {
        (New-Object -ComObject Shell.Application).MinimizeAll()
        Start-Sleep -Seconds 2
    }
    Step 'desktop' {
        Show-Desktop 'after'
        $work = [System.Windows.Forms.SystemInformation]::WorkingArea
        if ($work.Width -lt 1440 -or $work.Height -lt 940) {
            "::warning title=Showcase desktop::work area $($work.Width)x$($work.Height) : smaller than the main window of the showcase (1400x900 at 40,40)"
        }
    }
}

if ($Screenshot) {
    Step 'screenshot' {
        $size = [System.Windows.Forms.SystemInformation]::PrimaryMonitorSize
        $bitmap = New-Object System.Drawing.Bitmap $size.Width, $size.Height
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        $graphics.CopyFromScreen(0, 0, 0, 0, $bitmap.Size)
        $path = Join-Path (Get-Location) $Screenshot
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        "screenshot : $path"
    }
}
exit 0
