Set-StrictMode -Version 2
$ErrorActionPreference='Stop'

function Get-TestHttpsPlan {
    param([string]$DnsName,$ServeConfig)
    $dns=$DnsName.TrimEnd('.').ToLowerInvariant()
    if($dns -cnotmatch '^[a-z0-9][a-z0-9.-]*\.ts\.net$'){throw 'invalid_tailscale_dns'}
    if($null -eq $ServeConfig){throw 'invalid_serve_configuration'}
    $properties=$ServeConfig.PSObject.Properties
    if($properties['AllowFunnel']){
        foreach($entry in $ServeConfig.AllowFunnel.PSObject.Properties){if($entry.Value -eq $true){throw 'funnel_must_be_disabled'}}
    }
    $managed=$false
    if($properties['TCP'] -and $ServeConfig.TCP.PSObject.Properties['443']){
        $tcp=$ServeConfig.TCP.'443'
        if(!$tcp.PSObject.Properties['HTTPS'] -or $tcp.HTTPS -ne $true){throw 'https_port_in_use'}
        if(!$properties['Web']){throw 'https_port_in_use'}
        $web=$ServeConfig.Web.PSObject.Properties[$dns+':443']
        if(!$web -or !$web.Value.PSObject.Properties['Handlers']){throw 'https_port_in_use'}
        $handlers=@($web.Value.Handlers.PSObject.Properties)
        if($handlers.Count -ne 1 -or $handlers[0].Name -cne '/' -or !$handlers[0].Value.PSObject.Properties['Proxy'] -or $handlers[0].Value.Proxy -cne 'http://127.0.0.1:18740'){throw 'https_port_in_use'}
        foreach($entry in $ServeConfig.Web.PSObject.Properties){if($entry.Name.EndsWith(':443') -and $entry.Name -cne ($dns+':443')){throw 'https_port_in_use'}}
        $managed=$true
    }
    return [pscustomobject]@{public_origin='https://'+$dns;already_configured=$managed}
}
Export-ModuleMember -Function Get-TestHttpsPlan
