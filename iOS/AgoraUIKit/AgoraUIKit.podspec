Pod::Spec.new do |spec|
   spec.name          = "AgoraUIKit"
   spec.version       = "1.0"
   spec.summary       = "Agora iOS UIKit"
   spec.description   = "iOS UIKit"
   spec.homepage      = "https://docs.agora.io/en/Agora%20Platform/downloads"
   spec.license       = { "type" => "Copyright", "text" => "Copyright 2018 agora.io. All rights reserved.\n"}
   spec.author        = { "Agora Lab" => "developer@agora.io" }
   spec.platform      = :ios
   spec.source        = { :git => "" }
   spec.source_files = "**/*.swift"
   spec.requires_arc  = true
   spec.ios.deployment_target = '13.0'
   
 end