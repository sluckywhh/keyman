//
//  TestUtils.swift
//  KeymanEngineTests
//
//  Created by Joshua Horton on 2/19/20.
//  Copyright © 2020 SIL International. All rights reserved.
//

import Foundation
@testable import KeymanEngine

/**
 * This enum will 'namespace' the various testing utility functions used for our unit tests.
 * It's a common Swift pattern, used because enums can't be instantiated.
 */
enum TestUtils {
  static let mainBundle = Bundle(for: FileManagementTests.self)  // Can't use TestUtils, as it's an enum

  private static func findSubBundle(forResource resource: String, ofType type: String) -> Bundle {
    return Bundle(path: mainBundle.path(forResource: resource, ofType: type)!)!
  }

  static let keyboardsBundle = findSubBundle(forResource: "Keyboards", ofType: "bundle")
  static let lexicalModelsBundle = findSubBundle(forResource: "Lexical Models", ofType: "bundle")

  static func eraseStorage(_ storage: Storage) {
    do {
      try FileManager.default.removeItem(at: storage.baseDir)
    } catch {
      log.error(error)
    }
  }
}
